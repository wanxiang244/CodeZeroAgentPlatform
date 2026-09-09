package com.ljj.CodeZeroAgentPlatform.ai.model.message;

import dev.langchain4j.service.tool.ToolExecution;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 工具执行结果消息
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