package com.yupi.yuaicodemother.ai.model.message;

import dev.langchain4j.service.tool.ToolExecution;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 工具执行结果消息
 * 继承自 StreamMessage，表示工具执行完成后的结果
 * 当外部工具执行完成后，LangChain4j 会发送此类消息
 * 包含工具的标识、名称、调用参数和执行结果
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ToolExecutedMessage extends StreamMessage {

    /**
     * 工具调用的唯一标识 ID
     */
    private String id;

    /**
     * 工具名称
     */
    private String name;

    /**
     * 工具调用参数（JSON 格式）
     */
    private String arguments;

    /**
     * 工具执行结果
     */
    private String result;

    /**
     * 构造方法
     *
     * @param toolExecution LangChain4j 的工具执行对象
     */
    public ToolExecutedMessage(ToolExecution toolExecution) {
        // 设置消息类型为 TOOL_EXECUTED
        super(StreamMessageTypeEnum.TOOL_EXECUTED.getValue());
        this.id = toolExecution.request().id();
        this.name = toolExecution.request().name();
        this.arguments = toolExecution.request().arguments();
        this.result = toolExecution.result();
    }
}