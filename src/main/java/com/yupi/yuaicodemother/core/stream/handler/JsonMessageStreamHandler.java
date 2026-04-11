package com.yupi.yuaicodemother.core.stream.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.yupi.yuaicodemother.ai.model.message.AiResponseMessage;
import com.yupi.yuaicodemother.ai.model.message.StreamMessage;
import com.yupi.yuaicodemother.ai.model.message.StreamMessageTypeEnum;
import com.yupi.yuaicodemother.ai.model.message.ToolRequestMessage;
import com.yupi.yuaicodemother.core.stream.model.StreamProcessChunk;
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
 */
@Component
@Slf4j
public class JsonMessageStreamHandler implements StreamHandler {

    /**
     * 处理 JSON 消息流
     * 解析每条 JSON 消息，根据消息类型转换为对应的 StreamProcessChunk
     * 工具请求消息只在首次出现时输出，防止重复展示
     *
     * @param rawFlux 原始流
     * @return 处理后的结果流
     */
    @Override
    public Flux<StreamProcessChunk> handle(Flux<String> rawFlux) {
        // 使用 ConcurrentHashMap 的 keySet 来跟踪已输出过的工具 ID，保证线程安全
        Set<String> firstOutputToolIdSet = ConcurrentHashMap.newKeySet();
        return rawFlux.flatMap(rawMessage -> {
            StreamProcessChunk processChunk = parseMessage(rawMessage, firstOutputToolIdSet);
            // 过滤掉为空的消息
            if (processChunk == null || StrUtil.isAllBlank(processChunk.getResponseContent(), processChunk.getPersistenceContent())) {
                return Flux.empty();
            }
            return Flux.just(processChunk);
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
            case TOOL_EXECUTED -> null; // 工具执行结果不需要展示给用户
        };
    }
}
