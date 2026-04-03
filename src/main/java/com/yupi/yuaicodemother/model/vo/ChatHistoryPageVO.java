package com.yupi.yuaicodemother.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话历史游标分页结果
 */
@Data
public class ChatHistoryPageVO implements Serializable {

    /**
     * 消息列表
     */
    private List<ChatHistoryVO> records;

    /**
     * 是否还有更早的消息
     */
    private boolean hasMore;

    /**
     * 下一次查询游标时间
     */
    private LocalDateTime nextCursorCreateTime;

    /**
     * 下一次查询游标 id
     */
    private Long nextCursorId;

    private static final long serialVersionUID = 1L;
}
