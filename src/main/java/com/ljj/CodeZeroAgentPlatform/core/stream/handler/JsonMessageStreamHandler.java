package com.ljj.CodeZeroAgentPlatform.core.stream.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.ljj.CodeZeroAgentPlatform.ai.model.message.AiResponseMessage;
import com.ljj.CodeZeroAgentPlatform.ai.model.message.StreamMessage;
import com.ljj.CodeZeroAgentPlatform.ai.model.message.StreamMessageTypeEnum;
import com.ljj.CodeZeroAgentPlatform.ai.model.message.ToolExecutedMessage;
import com.ljj.CodeZeroAgentPlatform.ai.model.message.ToolRequestMessage;
import com.ljj.CodeZeroAgentPlatform.ai.tools.BaseTool;
import com.ljj.CodeZeroAgentPlatform.ai.tools.ToolManager;
import com.ljj.CodeZeroAgentPlatform.core.builder.VueProjectBuilder;
import com.ljj.CodeZeroAgentPlatform.core.stream.model.StreamProcessChunk;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSON 消息流处理器
 * 用于处理 Vue 项目生成的 JSON 格式流式响应
 * 解析 AI 响应消息、工具请求消息和工具执行消息，转换为 StreamProcessChunk
 * 工具请求消息只会首次出现时输出，避免重复展示相同的工具调用信息
 * 流式输出完成后，会自动触发 Vue 项目的构建流程
 */
@Component
@Slf4j
public class JsonMessageStreamHandler implements StreamHandler {

    /**
     * Vue 项目构建器，用于在流式输出完成后执行构建
     */
    @Resource
    private VueProjectBuilder vueProjectBuilder;

    /**
     * 工具管理器，用于根据工具名称获取工具实例并生成执行结果
     */
    @Resource
    private ToolManager toolManager;

    /**
     * 处理 JSON 消息流（无 appId）
     * 解析每条 JSON 消息，根据消息类型转换为对应的 StreamProcessChunk
     * 工具请求消息只在首次出现时输出，防止重复展示
     *
     * @param rawFlux 原始流
     * @return 处理后的结果流
     */
    @Override
    public Flux<StreamProcessChunk> handle(Flux<String> rawFlux) {
        return handle(rawFlux, null);
    }

    /**
     * 处理 JSON 消息流（支持 appId）
     * 解析每条 JSON 消息，根据消息类型转换为对应的 StreamProcessChunk
     * 流式输出完成后，使用 appId 执行 Vue 项目构建
     *
     * @param rawFlux 原始流
     * @param appId   应用 ID（用于 Vue 项目构建）
     * @return 处理后的结果流
     */
    @Override
    public Flux<StreamProcessChunk> handle(Flux<String> rawFlux, Long appId) {
        // 使用 ConcurrentHashMap 的 keySet 来跟踪已输出过的工具 ID，保证线程安全
        Set<String> firstOutputToolIdSet = ConcurrentHashMap.newKeySet();
        return rawFlux.flatMap(rawMessage -> {
            StreamProcessChunk processChunk = parseMessage(rawMessage, firstOutputToolIdSet);
            // 过滤掉为空的消息
            if (processChunk == null || StrUtil.isAllBlank(processChunk.getResponseContent(), processChunk.getPersistenceContent())) {
                return Flux.empty();
            }
            return Flux.just(processChunk);
        }).doOnComplete(() -> {
            // 流式输出完成后，执行 Vue 项目构建
            if (appId != null) {
                log.info("流式输出完成，开始构建 Vue 项目，appId: {}", appId);
                vueProjectBuilder.buildVueProjectSync(appId);
            }
        });
    }

    /**
     * 解析 JSON 消息
     * 根据消息类型进行不同的处理：
     * - AI_RESPONSE: 提取 AI 生成的文本内容
     * - TOOL_REQUEST: 提取工具名称，仅首次出现时输出
     * - TOOL_EXECUTED: 不输出，工具执行结果不需要展示给用户
     *
     * @param rawMessage            原始消息（JSON 格式或普通文本）
     * @param firstOutputToolIdSet  已输出过的工具 id 集合，用于去重
     * @return 处理结果，如果无需输出则返回 null
     */
    private StreamProcessChunk parseMessage(String rawMessage, Set<String> firstOutputToolIdSet) {
        // 非 JSON 格式的消息直接作为普通文本处理
        if (StrUtil.isBlank(rawMessage) || !JSONUtil.isTypeJSON(rawMessage)) {
            return new StreamProcessChunk(rawMessage, rawMessage);
        }
        StreamMessage streamMessage = JSONUtil.toBean(rawMessage, StreamMessage.class);
        StreamMessageTypeEnum messageTypeEnum = StreamMessageTypeEnum.getEnumByValue(streamMessage.getType());
        if (messageTypeEnum == null) {
            log.warn("未知的流消息类型：{}", rawMessage);
            return null;
        }
        return switch (messageTypeEnum) {
            case AI_RESPONSE -> {
                // AI 响应消息，提取文本内容返回给前端
                AiResponseMessage aiResponseMessage = JSONUtil.toBean(rawMessage, AiResponseMessage.class);
                yield new StreamProcessChunk(aiResponseMessage.getData(), aiResponseMessage.getData());
            }
            case TOOL_REQUEST -> {
                // 工具请求消息，提取工具名称，仅首次出现时输出
                ToolRequestMessage toolRequestMessage = JSONUtil.toBean(rawMessage, ToolRequestMessage.class);
                String toolId = toolRequestMessage.getId();
                // 如果没有 id，则使用工具名称作为标识
                if (StrUtil.isBlank(toolId)) {
                    toolId = toolRequestMessage.getName();
                }
                // 只有首次出现时才输出，避免重复展示相同的工具调用
                boolean firstOutput = StrUtil.isNotBlank(toolId) && firstOutputToolIdSet.add(toolId);
                if (!firstOutput) {
                    yield null;
                }
                String toolMessage = "选择工具：" + toolRequestMessage.getName();
                yield new StreamProcessChunk(toolMessage, toolMessage);
            }
            case TOOL_EXECUTED -> {
                // 工具执行结果消息，通过 ToolManager 获取工具实例并生成结果
                ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(rawMessage, ToolExecutedMessage.class);
                String toolName = toolExecutedMessage.getName();
                String arguments = toolExecutedMessage.getArguments();

                // 通过工具名称获取工具实例并生成结果格式
                BaseTool tool = toolManager.getTool(toolName);
                String result;
                if (tool != null) {
                    cn.hutool.json.JSONObject jsonArguments = cn.hutool.json.JSONUtil.parseObj(arguments);
                    result = tool.generateToolExecutedResult(jsonArguments);
                } else {
                    // 如果工具不存在，使用原始结果
                    result = toolExecutedMessage.getResult();
                    log.warn("未找到工具: {}", toolName);
                }

                yield new StreamProcessChunk(result, result);
            }
        };
    }
}
