package com.yupi.yuaicodemother.core.stream.handler;

import com.yupi.yuaicodemother.core.stream.model.StreamProcessChunk;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 原生文本流处理器
 */
@Component
public class SimpleTextStreamHandler implements StreamHandler {

    @Override
    public Flux<StreamProcessChunk> handle(Flux<String> rawFlux) {
        return rawFlux.map(chunk -> new StreamProcessChunk(chunk, chunk));
    }
}
