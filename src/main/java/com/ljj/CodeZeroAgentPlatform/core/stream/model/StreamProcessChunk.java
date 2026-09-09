package com.ljj.CodeZeroAgentPlatform.core.stream.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流处理结果块
 * 用于封装流式处理过程中的数据，包含返回给前端的内容和用于持久化拼接的内容
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
     * 该内容会用于构建完整的 AI 回复并保存到聊天历史
     */
    private String persistenceContent;
}
