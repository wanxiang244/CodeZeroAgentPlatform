package com.yupi.yuaicodemother.ai.model.message;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 工具调用请求消息
 * 继承自 StreamMessage，表示 AI 请求调用外部工具
 * 当 LangChain4j 检测到需要调用工具时，会发送此类消息
 * 包含工具的唯一标识、名称和调用参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ToolRequestMessage extends StreamMessage {

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
     * 构造方法
     *
     * @param toolExecutionRequest LangChain4j 的工具执行请求对象
     */
    public ToolRequestMessage(ToolExecutionRequest toolExecutionRequest) {
        // 设置消息类型为 TOOL_REQUEST
        super(StreamMessageTypeEnum.TOOL_REQUEST.getValue());
        this.id = toolExecutionRequest.id();
        this.name = toolExecutionRequest.name();
        this.arguments = toolExecutionRequest.arguments();
    }
}