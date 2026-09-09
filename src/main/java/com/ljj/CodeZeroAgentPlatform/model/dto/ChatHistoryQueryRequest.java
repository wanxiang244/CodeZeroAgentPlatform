package com.ljj.CodeZeroAgentPlatform.model.dto;

import com.ljj.CodeZeroAgentPlatform.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用对话历史查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChatHistoryQueryRequest extends PageRequest implements Serializable {

    /**
     * 应用 id
     */
    private Long appId;

    /**
     * 游标时间，加载更早的消息时使用
     */
    private LocalDateTime cursorCreateTime;

    /**
     * 游标 id，配合时间保证排序稳定
     */
    private Long cursorId;

    private static final long serialVersionUID = 1L;
}
