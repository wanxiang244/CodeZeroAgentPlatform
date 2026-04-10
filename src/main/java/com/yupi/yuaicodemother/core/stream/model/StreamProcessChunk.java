package com.yupi.yuaicodemother.core.stream.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流处理结果块
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreamProcessChunk {

    /**
     * 返回给前端的内容
     */
    private String responseContent;

    /**
     * 用于持久化拼接的内容
     */
    private String persistenceContent;
}
