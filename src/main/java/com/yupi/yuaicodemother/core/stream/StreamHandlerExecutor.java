package com.yupi.yuaicodemother.core.stream;

import com.yupi.yuaicodemother.core.stream.handler.JsonMessageStreamHandler;
import com.yupi.yuaicodemother.core.stream.handler.SimpleTextStreamHandler;
import com.yupi.yuaicodemother.core.stream.handler.StreamHandler;
import com.yupi.yuaicodemother.core.stream.model.StreamProcessChunk;
import com.yupi.yuaicodemother.exception.BusinessException;
import com.yupi.yuaicodemother.exception.ErrorCode;
import com.yupi.yuaicodemother.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 流处理执行器
 * 根据不同的代码生成类型选择对应的流处理器
 * - HTML 和 MULTI_FILE 类型使用 SimpleTextStreamHandler（简单文本处理）
 * - VUE_PROJECT 类型使用 JsonMessageStreamHandler（JSON 消息处理）
 */
@Component
public class StreamHandlerExecutor {

    @Resource
    private SimpleTextStreamHandler simpleTextStreamHandler;

    @Resource
    private JsonMessageStreamHandler jsonMessageStreamHandler;

    /**
     * 根据生成类型执行对应处理器
     *
     * @param rawFlux      原始流
     * @param codeGenType  代码生成类型
     * @return 处理结果流
     */
    public Flux<StreamProcessChunk> execute(Flux<String> rawFlux, CodeGenTypeEnum codeGenType) {
        // 根据代码生成类型选择合适的流处理器
        StreamHandler streamHandler = switch (codeGenType) {
            // HTML 和多文件代码生成使用简单文本处理器
            case HTML, MULTI_FILE -> simpleTextStreamHandler;
            // Vue 项目代码生成使用 JSON 消息处理器（支持工具调用信息）
            case VUE_PROJECT -> jsonMessageStreamHandler;
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的流处理类型：" + codeGenType.getValue());
        };
        return streamHandler.handle(rawFlux);
    }
}
