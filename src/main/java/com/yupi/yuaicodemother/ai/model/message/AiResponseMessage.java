package com.yupi.yuaicodemother.ai.model.message;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * AI 响应消息
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