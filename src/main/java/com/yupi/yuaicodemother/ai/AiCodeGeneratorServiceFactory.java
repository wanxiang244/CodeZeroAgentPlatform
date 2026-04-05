package com.yupi.yuaicodemother.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.yupi.yuaicodemother.model.entity.ChatHistory;
import com.yupi.yuaicodemother.model.enums.MessageTypeEnum;
import com.yupi.yuaicodemother.service.ChatHistoryService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
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

    @Resource
    private ChatModel chatModel;

    @Resource
    private StreamingChatModel streamingChatModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;

    /**
     * AI 服务实例缓存
     * 缓存策略：
     * - 最大缓存 1000 个实例
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    private final Cache<Long, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，appId: {}, 原因: {}", key, cause);
            })
            .build();

    /**
     * 根据 appId 获取服务（带缓存）
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return serviceCache.get(appId, this::createAiCodeGeneratorService);
    }

    /**
     * 创建新的 AI 服务实例
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId) {
        log.info("为 appId: {} 创建新的 AI 服务实例", appId);
        // 根据 appId 构建独立的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory
                .builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        initializeChatMemory(appId, chatMemory);
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemory(chatMemory)
                .build();
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

}
