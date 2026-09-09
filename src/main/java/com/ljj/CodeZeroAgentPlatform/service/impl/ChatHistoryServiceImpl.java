package com.ljj.CodeZeroAgentPlatform.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.ljj.CodeZeroAgentPlatform.exception.BusinessException;
import com.ljj.CodeZeroAgentPlatform.exception.ErrorCode;
import com.ljj.CodeZeroAgentPlatform.exception.ThrowUtils;
import com.ljj.CodeZeroAgentPlatform.mapper.AppMapper;
import com.ljj.CodeZeroAgentPlatform.mapper.ChatHistoryMapper;
import com.ljj.CodeZeroAgentPlatform.model.dto.ChatHistoryAdminQueryRequest;
import com.ljj.CodeZeroAgentPlatform.model.dto.ChatHistoryQueryRequest;
import com.ljj.CodeZeroAgentPlatform.model.entity.App;
import com.ljj.CodeZeroAgentPlatform.model.entity.ChatHistory;
import com.ljj.CodeZeroAgentPlatform.model.entity.User;
import com.ljj.CodeZeroAgentPlatform.model.enums.MessageTypeEnum;
import com.ljj.CodeZeroAgentPlatform.model.enums.UserRoleEnum;
import com.ljj.CodeZeroAgentPlatform.model.vo.ChatHistoryPageVO;
import com.ljj.CodeZeroAgentPlatform.model.vo.ChatHistoryVO;
import com.ljj.CodeZeroAgentPlatform.model.vo.UserVO;
import com.ljj.CodeZeroAgentPlatform.service.ChatHistoryService;
import com.ljj.CodeZeroAgentPlatform.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 对话历史 服务层实现。
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 */
@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory> implements ChatHistoryService {

    private static final int MAX_PAGE_SIZE = 20;

    @Resource
    private UserService userService;

    @Resource
    private AppMapper appMapper;

    @Override
    public ChatHistory saveMessage(Long appId, Long userId, MessageTypeEnum messageType, String message) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 id 非法");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户 id 非法");
        ThrowUtils.throwIf(messageType == null, ErrorCode.PARAMS_ERROR, "消息类型不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息内容不能为空");

        ChatHistory chatHistory = ChatHistory.builder()
                .appId(appId)
                .userId(userId)
                .messageType(messageType.getValue())
                .message(message)
                .build();
        boolean result = this.save(chatHistory);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "保存对话历史失败");
        return chatHistory;
    }

    @Override
    public ChatHistoryPageVO listAppChatHistoryByPage(ChatHistoryQueryRequest chatHistoryQueryRequest, User loginUser) {
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        Long appId = chatHistoryQueryRequest.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 id 非法");
        validateAppReadPermission(appId, loginUser);

        LocalDateTime cursorCreateTime = chatHistoryQueryRequest.getCursorCreateTime();
        Long cursorId = chatHistoryQueryRequest.getCursorId();
        if ((cursorCreateTime == null) != (cursorId == null)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "游标参数不完整");
        }

        long pageSize = Math.min(Math.max(chatHistoryQueryRequest.getPageSize(), 1), MAX_PAGE_SIZE);
        List<ChatHistory> chatHistoryList = mapper.selectAppHistoryPage(appId, cursorCreateTime, cursorId, pageSize + 1);

        boolean hasMore = chatHistoryList.size() > pageSize;
        if (hasMore) {
            chatHistoryList = new ArrayList<>(chatHistoryList.subList(0, (int) pageSize));
        }

        List<ChatHistoryVO> chatHistoryVOList = getChatHistoryVOList(chatHistoryList);
        chatHistoryVOList.sort(Comparator
                .comparing(ChatHistoryVO::getCreateTime)
                .thenComparing(ChatHistoryVO::getId));

        ChatHistoryPageVO pageVO = new ChatHistoryPageVO();
        pageVO.setRecords(chatHistoryVOList);
        pageVO.setHasMore(hasMore);
        if (hasMore && CollUtil.isNotEmpty(chatHistoryVOList)) {
            ChatHistoryVO oldestChatHistory = chatHistoryVOList.get(0);
            pageVO.setNextCursorCreateTime(oldestChatHistory.getCreateTime());
            pageVO.setNextCursorId(oldestChatHistory.getId());
        }
        return pageVO;
    }

    @Override
    public Page<ChatHistoryVO> listChatHistoryByPageForAdmin(ChatHistoryAdminQueryRequest chatHistoryAdminQueryRequest) {
        ThrowUtils.throwIf(chatHistoryAdminQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = Math.max(chatHistoryAdminQueryRequest.getPageNum(), 1);
        long pageSize = Math.min(Math.max(chatHistoryAdminQueryRequest.getPageSize(), 1), MAX_PAGE_SIZE);
        Page<ChatHistory> chatHistoryPage = this.page(Page.of(pageNum, pageSize), getAdminQueryWrapper(chatHistoryAdminQueryRequest));
        Page<ChatHistoryVO> resultPage = new Page<>(pageNum, pageSize, chatHistoryPage.getTotalRow());
        resultPage.setRecords(getChatHistoryVOList(chatHistoryPage.getRecords()));
        return resultPage;
    }

    @Override
    public QueryWrapper getAdminQueryWrapper(ChatHistoryAdminQueryRequest chatHistoryAdminQueryRequest) {
        if (chatHistoryAdminQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        String sortField = chatHistoryAdminQueryRequest.getSortField();
        String sortOrder = chatHistoryAdminQueryRequest.getSortOrder();
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", chatHistoryAdminQueryRequest.getAppId())
                .eq("userId", chatHistoryAdminQueryRequest.getUserId())
                .eq("messageType", chatHistoryAdminQueryRequest.getMessageType())
                .like("message", chatHistoryAdminQueryRequest.getMessage());
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            queryWrapper.orderBy("createTime", false).orderBy("id", false);
        }
        return queryWrapper;
    }

    @Override
    public boolean removeByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 id 非法");
        mapper.logicalDeleteByAppId(appId);
        return true;
    }

    @Override
    public ChatHistoryVO getChatHistoryVO(ChatHistory chatHistory) {
        if (chatHistory == null) {
            return null;
        }
        List<ChatHistoryVO> chatHistoryVOList = getChatHistoryVOList(List.of(chatHistory));
        return chatHistoryVOList.isEmpty() ? null : chatHistoryVOList.get(0);
    }

    @Override
    public List<ChatHistoryVO> getChatHistoryVOList(List<ChatHistory> chatHistoryList) {
        if (CollUtil.isEmpty(chatHistoryList)) {
            return new ArrayList<>();
        }
        List<ChatHistoryVO> chatHistoryVOList = chatHistoryList.stream()
                .filter(Objects::nonNull)
                .map(chatHistory -> {
                    ChatHistoryVO chatHistoryVO = new ChatHistoryVO();
                    BeanUtil.copyProperties(chatHistory, chatHistoryVO);
                    return chatHistoryVO;
                })
                .collect(Collectors.toList());
        fillUserInfo(chatHistoryVOList);
        fillAppInfo(chatHistoryVOList);
        return chatHistoryVOList;
    }

    @Override
    public List<ChatHistory> listAppChatHistoryForMemory(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 id 非法");
        List<ChatHistory> chatHistoryList = mapper.selectAppHistoryForMemory(appId);
        if (CollUtil.isEmpty(chatHistoryList)) {
            return Collections.emptyList();
        }
        return chatHistoryList;
    }

    /**
     * 校验当前用户是否有权限查看应用对话历史
     *
     * @param appId      应用 id
     * @param loginUser  当前登录用户
     */
    private void validateAppReadPermission(Long appId, User loginUser) {
        App app = appMapper.selectOneById(appId);
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }
        boolean isAdmin = UserRoleEnum.ADMIN.getValue().equals(loginUser.getUserRole());
        boolean isOwner = app.getUserId().equals(loginUser.getId());
        if (!isOwner && !isAdmin) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限查看该应用的对话历史");
        }
    }

    /**
     * 批量填充用户信息
     *
     * @param chatHistoryVOList 对话历史视图列表
     */
    private void fillUserInfo(List<ChatHistoryVO> chatHistoryVOList) {
        Set<Long> userIdSet = chatHistoryVOList.stream()
                .map(ChatHistoryVO::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIdSet.isEmpty()) {
            return;
        }
        Map<Long, UserVO> userVOMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        chatHistoryVOList.forEach(chatHistoryVO -> chatHistoryVO.setUser(userVOMap.get(chatHistoryVO.getUserId())));
    }

    /**
     * 批量填充应用信息
     *
     * @param chatHistoryVOList 对话历史视图列表
     */
    private void fillAppInfo(List<ChatHistoryVO> chatHistoryVOList) {
        Map<Long, App> appCache = new HashMap<>();
        for (ChatHistoryVO chatHistoryVO : chatHistoryVOList) {
            Long appId = chatHistoryVO.getAppId();
            if (appId == null) {
                continue;
            }
            App app = appCache.computeIfAbsent(appId, appMapper::selectOneById);
            if (app != null) {
                chatHistoryVO.setAppName(app.getAppName());
            }
        }
    }
}
