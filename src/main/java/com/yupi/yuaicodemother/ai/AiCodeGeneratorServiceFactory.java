package com.yupi.yuaicodemother.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yupi.yuaicodemother.ai.guardrail.PromptSafetyInputGuardrail;
import com.yupi.yuaicodemother.ai.tools.FileWriteTool;
import com.yupi.yuaicodemother.ai.tools.ToolManager;
import com.yupi.yuaicodemother.exception.BusinessException;
import com.yupi.yuaicodemother.exception.ErrorCode;
import com.yupi.yuaicodemother.model.entity.ChatHistory;
import com.yupi.yuaicodemother.model.enums.CodeGenTypeEnum;
import com.yupi.yuaicodemother.model.enums.MessageTypeEnum;
import com.yupi.yuaicodemother.service.ChatHistoryService;
import com.yupi.yuaicodemother.utils.SpringContextUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.time.Duration;
import java.util.List;

/**
 * AI 服务创建工厂
 */
@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {

    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private ToolManager toolManager;

    /**
     * AI 服务实例缓存
     * 缓存策略：
     * - 最大缓存 1000 个实例
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，缓存键: {}, 原因: {}", key, cause);
            })
            .build();

    /**
     * 根据 appId 获取服务（为了兼容老逻辑）
     *
     * @param appId
     * @return
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }

    /**
     * 根据 appId 获取服务
     *
     * @param appId       应用 id
     * @param codeGenType 生成类型
     * @return
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        String cacheKey = buildCacheKey(appId, codeGenType);
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, codeGenType));
    }

    /**
     * 创建新的 AI 服务实例
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        // 根据 appId 构建独立的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(50)
                .build();
        // 从数据库加载历史对话到记忆中
        initializeChatMemory(appId, chatMemory);
        // 根据代码生成类型选择不同的模型配置
        return switch (codeGenType) {
            // Vue 项目生成，使用工具调用和推理模型
            case VUE_PROJECT -> {
                // 使用多例模式的 StreamingChatModel 解决并发问题
                StreamingChatModel reasoningStreamingChatModel = SpringContextUtil.getBean("reasoningStreamingChatModelPrototype", StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(chatModel)
                        .streamingChatModel(reasoningStreamingChatModel)
                        .chatMemoryProvider(memoryId -> chatMemory)
                        .tools(toolManager.getAllTools())
                        // 处理工具调用幻觉问题
                        .hallucinatedToolNameStrategy(toolExecutionRequest ->
                                ToolExecutionResultMessage.from(toolExecutionRequest,
                                        "Error: there is no tool called " + toolExecutionRequest.name())
                        ).inputGuardrails(new PromptSafetyInputGuardrail()) // 添加输入护轨

                        .build();
            }
            // HTML 和 多文件生成，使用流式对话模型
            case HTML, MULTI_FILE -> {
                // 使用多例模式的 StreamingChatModel 解决并发问题
                StreamingChatModel openAiStreamingChatModel = SpringContextUtil.getBean("streamingChatModelPrototype", StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(chatModel)
                        .streamingChatModel(openAiStreamingChatModel)
                        .chatMemory(chatMemory)
                        .maxSequentialToolsInvocations(20)  // 最多连续调用 20 次工具
                        .inputGuardrails(new PromptSafetyInputGuardrail()) // 添加输入护轨
                        .build();
            }
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "不支持的代码生成类型: " + codeGenType.getValue());
        };
    }

    /**
     * 初始化对话记忆
     * 1. 先清理 Redis 中已有的历史记忆，避免重复加载
     * 2. 从数据库查询历史消息，跳过最新一条用户消息，避免与当前请求重复
     * 3. 将倒序结果翻转为正序，再逐条加载到记忆中
     *
     * @param appId      应用 id
     * @param chatMemory 对话记忆
     */
    private void initializeChatMemory(long appId, MessageWindowChatMemory chatMemory) {
        chatMemory.clear();
        List<ChatHistory> chatHistoryList = chatHistoryService.listAppChatHistoryForMemory(appId);
        if (chatHistoryList.isEmpty()) {
            return;
        }
        if (chatHistoryList.size() <= 1) {
            log.debug("appId: {} 历史消息不足，无需初始化记忆", appId);
            return;
        }

        List<ChatHistory> historyToLoad = new ArrayList<>(chatHistoryList.subList(1, chatHistoryList.size()));
        Collections.reverse(historyToLoad);
        for (ChatHistory chatHistory : historyToLoad) {
            ChatMessage chatMessage = toChatMessage(chatHistory);
            if (chatMessage != null) {
                chatMemory.add(chatMessage);
            }
        }
        log.info("appId: {} 对话记忆初始化完成，加载历史消息 {} 条", appId, historyToLoad.size());
    }

    /**
     * 转换对话历史为 LangChain4j 消息
     *
     * @param chatHistory 对话历史
     * @return ChatMessage
     */
    private ChatMessage toChatMessage(ChatHistory chatHistory) {
        if (chatHistory == null || chatHistory.getMessage() == null) {
            return null;
        }
        MessageTypeEnum messageTypeEnum = MessageTypeEnum.getEnumByValue(chatHistory.getMessageType());
        if (messageTypeEnum == null) {
            return null;
        }
        return switch (messageTypeEnum) {
            case USER -> UserMessage.from(chatHistory.getMessage());
            case AI -> AiMessage.from(chatHistory.getMessage());
            case ERROR -> SystemMessage.from(chatHistory.getMessage());
        };
    }

    /**
     * 构造缓存键
     *
     * @param appId
     * @param codeGenType
     * @return
     */
    private String buildCacheKey(long appId, CodeGenTypeEnum codeGenType) {
        return appId + "_" + codeGenType.getValue();
    }

}
