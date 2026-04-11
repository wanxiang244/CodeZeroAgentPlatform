package com.yupi.yuaicodemother.core.stream.handler;

import com.yupi.yuaicodemother.core.stream.model.StreamProcessChunk;
import reactor.core.publisher.Flux;

/**
 * Flux 流处理器接口
 * 定义流式处理器的统一接口，用于处理不同类型的流式响应
 * 支持简单文本流和 JSON 消息流等多种处理模式
 */
public interface StreamHandler {

    /**
     * 处理原始流
     *
     * @param rawFlux 原始流
     * @return 处理后的结果流
     */
    Flux<StreamProcessChunk> handle(Flux<String> rawFlux);
}
