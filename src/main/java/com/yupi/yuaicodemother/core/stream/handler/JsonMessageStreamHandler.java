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
 */
@Component
@Slf4j
public class JsonMessageStreamHandler implements StreamHandler {

    @Override
    public Flux<StreamProcessChunk> handle(Flux<String> rawFlux) {
        Set<String> firstOutputToolIdSet = ConcurrentHashMap.newKeySet();
        return rawFlux.flatMap(rawMessage -> {
            StreamProcessChunk processChunk = parseMessage(rawMessage, firstOutputToolIdSet);
            if (processChunk == null || StrUtil.isAllBlank(processChunk.getResponseContent(), processChunk.getPersistenceContent())) {
                return Flux.empty();
            }
            return Flux.just(processChunk);
        });
    }

    /**
     * 解析 JSON 消息
     *
     * @param rawMessage            原始消息
     * @param firstOutputToolIdSet  已输出过的工具 id 集合
     * @return 处理结果
     */
    private StreamProcessChunk parseMessage(String rawMessage, Set<String> firstOutputToolIdSet) {
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
                AiResponseMessage aiResponseMessage = JSONUtil.toBean(rawMessage, AiResponseMessage.class);
                yield new StreamProcessChunk(aiResponseMessage.getData(), aiResponseMessage.getData());
            }
            case TOOL_REQUEST -> {
                ToolRequestMessage toolRequestMessage = JSONUtil.toBean(rawMessage, ToolRequestMessage.class);
                String toolId = toolRequestMessage.getId();
                if (StrUtil.isBlank(toolId)) {
                    toolId = toolRequestMessage.getName();
                }
                boolean firstOutput = StrUtil.isNotBlank(toolId) && firstOutputToolIdSet.add(toolId);
                if (!firstOutput) {
                    yield null;
                }
                String toolMessage = "选择工具：" + toolRequestMessage.getName();
                yield new StreamProcessChunk(toolMessage, toolMessage);
            }
            case TOOL_EXECUTED -> null;
        };
    }
}
