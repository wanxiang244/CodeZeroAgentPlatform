package com.yupi.yuaicodemother.ai.model.message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * AI 响应消息
 * 继承自 StreamMessage，表示 AI 生成的文本响应
 * 在流式输出时，AI 每生成一个文本片段就会发送一条此类消息
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class AiResponseMessage extends StreamMessage {

    /**
     * AI 生成的文本内容
     */
    private String data;

    /**
     * 构造方法
     *
     * @param data AI 生成的文本内容
     */
    public AiResponseMessage(String data) {
        // 设置消息类型为 AI_RESPONSE
        super(StreamMessageTypeEnum.AI_RESPONSE.getValue());
        this.data = data;
    }
}