package com.yupi.yuaicodemother.core.stream.handler;

import com.yupi.yuaicodemother.core.stream.model.StreamProcessChunk;
import reactor.core.publisher.Flux;

/**
 * Flux 流处理器
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
