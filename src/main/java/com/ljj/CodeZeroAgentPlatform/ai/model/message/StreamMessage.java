package com.ljj.CodeZeroAgentPlatform.ai.model.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流式消息响应基类
 * 所有流式消息类型的基类，包含消息类型字段
 * 子类包括：AiResponseMessage、ToolRequestMessage、ToolExecutedMessage
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StreamMessage {

    /**
     * 消息类型
     * 对应 StreamMessageTypeEnum 枚举值，用于区分不同的消息类型
     */
    private String type;
}