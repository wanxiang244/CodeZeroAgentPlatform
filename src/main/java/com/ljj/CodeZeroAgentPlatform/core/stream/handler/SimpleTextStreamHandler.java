package com.ljj.CodeZeroAgentPlatform.core.stream.handler;

import com.ljj.CodeZeroAgentPlatform.core.stream.model.StreamProcessChunk;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 原生文本流处理器
 * 用于处理简单的文本流式响应，直接将原始字符串转换为 StreamProcessChunk
 * 适用于 HTML 和多文件代码生成场景，返回给前端的内容和持久化内容相同
 */
@Component
public class SimpleTextStreamHandler implements StreamHandler {

    /**
     * 处理原始文本流（无 appId）
     * 将每个字符串块直接封装为 StreamProcessChunk，responseContent 和 persistenceContent 相同
     *
     * @param rawFlux 原始流
     * @return 处理后的结果流
     */
    @Override
    public Flux<StreamProcessChunk> handle(Flux<String> rawFlux) {
        return handle(rawFlux, null);
    }

    /**
     * 处理原始文本流（支持 appId）
     * SimpleTextStreamHandler 不需要 appId，直接忽略该参数
     *
     * @param rawFlux 原始流
     * @param appId   应用 ID（对于简单文本处理器无意义，直接忽略）
     * @return 处理后的结果流
     */
    @Override
    public Flux<StreamProcessChunk> handle(Flux<String> rawFlux, Long appId) {
        return rawFlux.map(chunk -> new StreamProcessChunk(chunk, chunk));
    }
}
