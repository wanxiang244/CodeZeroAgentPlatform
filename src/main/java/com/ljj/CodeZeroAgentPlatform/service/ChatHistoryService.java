package com.ljj.CodeZeroAgentPlatform.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.ljj.CodeZeroAgentPlatform.model.dto.ChatHistoryAdminQueryRequest;
import com.ljj.CodeZeroAgentPlatform.model.dto.ChatHistoryQueryRequest;
import com.ljj.CodeZeroAgentPlatform.model.entity.ChatHistory;
import com.ljj.CodeZeroAgentPlatform.model.entity.User;
import com.ljj.CodeZeroAgentPlatform.model.enums.MessageTypeEnum;
import com.ljj.CodeZeroAgentPlatform.model.vo.ChatHistoryPageVO;
import com.ljj.CodeZeroAgentPlatform.model.vo.ChatHistoryVO;

import java.util.List;

/**
 * 对话历史 服务层。
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 保存对话消息
     *
     * @param appId        应用 id
     * @param userId       用户 id
     * @param messageType  消息类型
     * @param message      消息内容
     * @return 保存后的消息
     */
    ChatHistory saveMessage(Long appId, Long userId, MessageTypeEnum messageType, String message);

    /**
     * 查询应用对话历史（游标分页）
     *
     * @param chatHistoryQueryRequest 查询条件
     * @param loginUser               当前登录用户
     * @return 游标分页结果
     */
    ChatHistoryPageVO listAppChatHistoryByPage(ChatHistoryQueryRequest chatHistoryQueryRequest, User loginUser);

    /**
     * 管理员分页查询所有对话历史
     *
     * @param chatHistoryAdminQueryRequest 查询条件
     * @return 分页结果
     */
    Page<ChatHistoryVO> listChatHistoryByPageForAdmin(ChatHistoryAdminQueryRequest chatHistoryAdminQueryRequest);

    /**
     * 构造管理员查询条件
     *
     * @param chatHistoryAdminQueryRequest 查询条件
     * @return 查询包装器
     */
    QueryWrapper getAdminQueryWrapper(ChatHistoryAdminQueryRequest chatHistoryAdminQueryRequest);

    /**
     * 删除应用下的所有对话历史
     *
     * @param appId 应用 id
     * @return 是否删除成功
     */
    boolean removeByAppId(Long appId);

    /**
     * 获取单条视图对象
     *
     * @param chatHistory 对话历史
     * @return 视图对象
     */
    ChatHistoryVO getChatHistoryVO(ChatHistory chatHistory);

    /**
     * 批量获取视图对象
     *
     * @param chatHistoryList 对话历史列表
     * @return 视图对象列表
     */
    List<ChatHistoryVO> getChatHistoryVOList(List<ChatHistory> chatHistoryList);

    /**
     * 查询应用全部对话历史，供记忆初始化使用
     * 返回结果按时间倒序排列
     *
     * @param appId 应用 id
     * @return 对话历史列表
     */
    List<ChatHistory> listAppChatHistoryForMemory(Long appId);
}
