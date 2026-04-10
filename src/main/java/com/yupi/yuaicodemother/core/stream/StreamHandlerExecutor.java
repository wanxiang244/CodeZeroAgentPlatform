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
        StreamHandler streamHandler = switch (codeGenType) {
            case HTML, MULTI_FILE -> simpleTextStreamHandler;
            case VUE_PROJECT -> jsonMessageStreamHandler;
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的流处理类型：" + codeGenType.getValue());
        };
        return streamHandler.handle(rawFlux);
    }
}
