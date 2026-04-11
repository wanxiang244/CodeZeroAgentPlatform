package com.yupi.yuaicodemother.ai.model.message;

import lombok.Getter;

/**
 * 流式消息类型枚举
 * 定义三种流式消息类型：
 * - AI_RESPONSE: AI 生成的文本响应
 * - TOOL_REQUEST: 工具调用请求（当 AI 需要调用工具时触发）
 * - TOOL_EXECUTED: 工具执行完成（工具执行结果）
 */
@Getter
public enum StreamMessageTypeEnum {

    AI_RESPONSE("ai_response", "AI响应"),
    TOOL_REQUEST("tool_request", "工具请求"),
    TOOL_EXECUTED("tool_executed", "工具执行结果");

    private final String value;
    private final String text;

    /**
     * 构造方法
     *
     * @param value 枚举值，用于 JSON 序列化
     * @param text  枚举描述
     */
    StreamMessageTypeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据值获取枚举
     *
     * @param value 枚举值
     * @return 对应的枚举对象，如果未找到则返回 null
     */
    public static StreamMessageTypeEnum getEnumByValue(String value) {
        for (StreamMessageTypeEnum typeEnum : values()) {
            if (typeEnum.getValue().equals(value)) {
                return typeEnum;
            }
        }
        return null;
    }
}