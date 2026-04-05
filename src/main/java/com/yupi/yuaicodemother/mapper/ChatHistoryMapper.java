package com.yupi.yuaicodemother.mapper;

import com.mybatisflex.core.BaseMapper;
import com.yupi.yuaicodemother.model.entity.ChatHistory;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话历史 映射层。
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 */
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {

    /**
     * 游标分页查询应用对话历史
     *
     * @param appId            应用 id
     * @param cursorCreateTime 游标时间
     * @param cursorId         游标 id
     * @param limit            查询数量
     * @return 对话历史列表
     */
    List<ChatHistory> selectAppHistoryPage(@Param("appId") Long appId,
                                           @Param("cursorCreateTime") LocalDateTime cursorCreateTime,
                                           @Param("cursorId") Long cursorId,
                                           @Param("limit") long limit);

    /**
     * 按应用 id 逻辑删除对话历史
     *
     * @param appId 应用 id
     * @return 影响行数
     */
    int logicalDeleteByAppId(@Param("appId") Long appId);

    /**
     * 查询应用全部对话历史，按时间倒序
     *
     * @param appId 应用 id
     * @return 对话历史列表
     */
    List<ChatHistory> selectAppHistoryForMemory(@Param("appId") Long appId);
}
