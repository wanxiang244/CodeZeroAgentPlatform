package com.yupi.yuaicodemother.core;

import com.yupi.yuaicodemother.ai.AiCodeGeneratorService;
import com.yupi.yuaicodemother.ai.AiCodeGeneratorServiceFactory;
import com.yupi.yuaicodemother.ai.model.HtmlCodeResult;
import com.yupi.yuaicodemother.ai.model.MultiFileCodeResult;
import com.yupi.yuaicodemother.ai.model.message.AiResponseMessage;
import com.yupi.yuaicodemother.ai.model.message.ToolExecutedMessage;
import com.yupi.yuaicodemother.ai.model.message.ToolRequestMessage;
import com.yupi.yuaicodemother.core.parser.CodeParserExecutor;
import com.yupi.yuaicodemother.core.saver.CodeFileSaverExecutor;
import com.yupi.yuaicodemother.core.stream.StreamHandlerExecutor;
import com.yupi.yuaicodemother.core.stream.model.StreamProcessChunk;
import com.yupi.yuaicodemother.exception.BusinessException;
import com.yupi.yuaicodemother.exception.ErrorCode;
import com.yupi.yuaicodemother.model.enums.CodeGenTypeEnum;

import cn.hutool.json.JSONUtil;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI 代码生成门面类，组合代码生成和保存功能
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;

    /**
     * 统一入口：根据类型生成并保存代码
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
        return generateAndSaveCode(userMessage, codeGenTypeEnum, null);
    }

    /**
     * 统一入口：根据类型生成并保存代码（支持 appId）
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId           应用 id（可选，用于关联应用）
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型不能为空");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult result = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId)
                        .generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult result = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId)
                        .generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(result, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 统一入口：根据类型生成并保存代码（流式）
     * 支持 HTML、多文件、Vue 项目三种代码生成类型的流式处理
     * 返回 Flux<StreamProcessChunk>，包含响应内容和持久化内容
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 流式响应
     */
    public Flux<StreamProcessChunk> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
        return generateAndSaveCodeStream(userMessage, codeGenTypeEnum, null);
    }

    /**
     * 统一入口：根据类型生成并保存代码（流式，支持 appId）
     * 根据不同的代码生成类型调用不同的 AI 服务：
     * - HTML: 调用 generateHtmlCodeStream，返回 Flux<String>
     * - MULTI_FILE: 调用 generateMultiFileCodeStream，返回 Flux<String>
     * - VUE_PROJECT: 调用 generateVueProjectCodeStream，返回 TokenStream（支持工具调用）
     * 处理完成后返回 StreamProcessChunk 流，并在末尾追加 [DONE] 标记
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @param appId           应用 id（可选，用于关联应用）
     * @return 流式响应
     */
    public Flux<StreamProcessChunk> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成类型不能为空");
        }
        // 根据 appId 和 codeGenType 获取对应的 AI 服务实例
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId,
                codeGenTypeEnum);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                // HTML 代码生成：调用流式接口，处理原始流，构建处理后的流
                Flux<String> codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                Flux<String> rawFlux = processCodeStream(codeStream, CodeGenTypeEnum.HTML, appId);
                yield buildHandledFlux(rawFlux, CodeGenTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                // 多文件代码生成：调用流式接口，处理原始流，构建处理后的流
                Flux<String> codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                Flux<String> rawFlux = processCodeStream(codeStream, CodeGenTypeEnum.MULTI_FILE, appId);
                yield buildHandledFlux(rawFlux, CodeGenTypeEnum.MULTI_FILE, appId);
            }
            case VUE_PROJECT -> {
                // Vue 项目代码生成：调用 TokenStream 接口，支持工具调用
                TokenStream tokenStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
                Flux<String> rawFlux = processTokenStream(tokenStream);
                yield buildHandledFlux(rawFlux, CodeGenTypeEnum.VUE_PROJECT, appId);
            }
            default -> {
                String errorMessage = "不支持的生成类型：" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }

    /**
     * 使用执行器处理原始流，并在末尾追加完成标记 [DONE]
     * 便于前端识别流式响应的结束位置
     *
     * @param rawFlux     原始流
     * @param codeGenType 代码生成类型
     * @return 处理后的流
     */
    private Flux<StreamProcessChunk> buildHandledFlux(Flux<String> rawFlux, CodeGenTypeEnum codeGenType) {
        return buildHandledFlux(rawFlux, codeGenType, null);
    }

    /**
     * 使用执行器处理原始流，并在末尾追加完成标记 [DONE]
     * 便于前端识别流式响应的结束位置
     * appId 用于 Vue 项目构建时定位项目目录
     *
     * @param rawFlux     原始流
     * @param codeGenType 代码生成类型
     * @param appId       应用 ID（可选，用于 Vue 项目构建）
     * @return 处理后的流
     */
    private Flux<StreamProcessChunk> buildHandledFlux(Flux<String> rawFlux, CodeGenTypeEnum codeGenType, Long appId) {
        return streamHandlerExecutor.execute(rawFlux, codeGenType, appId)
                .concatWith(Flux.just(new StreamProcessChunk("[DONE]", "")));
    }

    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     * 监听 TokenStream 的各种事件：
     * - onPartialResponse: AI 生成的文本片段
     * - onPartialToolExecutionRequest: 工具调用请求
     * - onToolExecuted: 工具执行完成
     * - onCompleteResponse: 完整响应完成
     * - onError: 错误处理
     * 每种事件都会转换为对应的 JSON 消息格式发送给前端
     *
     * @param tokenStream TokenStream 对象
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream) {
        return Flux.create(sink -> {
            // AI 生成的文本片段
            tokenStream.onPartialResponse((String partialResponse) -> {
                AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                sink.next(JSONUtil.toJsonStr(aiResponseMessage));
            })
                    // 工具调用请求
                    .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    // 工具执行完成
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    // 完整响应完成
                    .onCompleteResponse((ChatResponse response) -> {
                        sink.complete();
                    })
                    // 错误处理
                    .onError((Throwable error) -> {
                        error.printStackTrace();
                        sink.error(error);
                    })
                    .start();
        });
    }

    /**
     * 通用流式代码处理方法
     *
     * @param codeStream  代码流
     * @param codeGenType 代码生成类型
     * @param appId       应用 id（可选，用于关联应用）
     * @return 流式响应
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appId) {
        // 字符串拼接器，用于当流式返回所有的代码之后，再保存代码
        StringBuilder codeBuilder = new StringBuilder();
        return codeStream.doOnNext(chunk -> {
            // 实时收集代码片段
            codeBuilder.append(chunk);
        }).doOnComplete(() -> {
            // 流式返回完成后，保存代码
            try {
                String completeCode = codeBuilder.toString();
                // 使用执行器解析代码
                Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
                // 使用执行器保存代码
                File saveDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                log.info("保存成功，目录为：{}", saveDir.getAbsolutePath());
            } catch (Exception e) {
                log.error("保存失败: {}", e.getMessage());
            }
        });
    }
}
