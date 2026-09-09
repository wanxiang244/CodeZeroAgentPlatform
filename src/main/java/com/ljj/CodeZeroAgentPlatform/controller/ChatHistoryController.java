package com.ljj.CodeZeroAgentPlatform.controller;

import com.mybatisflex.core.paginate.Page;
import com.ljj.CodeZeroAgentPlatform.annotation.AuthCheck;
import com.ljj.CodeZeroAgentPlatform.common.BaseResponse;
import com.ljj.CodeZeroAgentPlatform.common.ResultUtils;
import com.ljj.CodeZeroAgentPlatform.constant.UserConstant;
import com.ljj.CodeZeroAgentPlatform.exception.ErrorCode;
import com.ljj.CodeZeroAgentPlatform.exception.ThrowUtils;
import com.ljj.CodeZeroAgentPlatform.model.dto.ChatHistoryAdminQueryRequest;
import com.ljj.CodeZeroAgentPlatform.model.dto.ChatHistoryQueryRequest;
import com.ljj.CodeZeroAgentPlatform.model.entity.User;
import com.ljj.CodeZeroAgentPlatform.model.vo.ChatHistoryPageVO;
import com.ljj.CodeZeroAgentPlatform.model.vo.ChatHistoryVO;
import com.ljj.CodeZeroAgentPlatform.service.ChatHistoryService;
import com.ljj.CodeZeroAgentPlatform.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对话历史 控制层。
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 */
@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private UserService userService;

    /**
     * 分页查询应用对话历史（仅应用创建者和管理员可见）
     *
     * @param chatHistoryQueryRequest 查询条件
     * @param request                 HTTP 请求
     * @return 对话历史游标分页结果
     */
    @PostMapping("/app/list/page")
    public BaseResponse<ChatHistoryPageVO> listAppChatHistoryByPage(@RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest,
                                                                    HttpServletRequest request) {
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(chatHistoryService.listAppChatHistoryByPage(chatHistoryQueryRequest, loginUser));
    }

    /**
     * 管理员分页查询所有对话历史
     *
     * @param chatHistoryAdminQueryRequest 查询条件
     * @return 对话历史分页结果
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistoryVO>> listChatHistoryByPageForAdmin(
            @RequestBody ChatHistoryAdminQueryRequest chatHistoryAdminQueryRequest) {
        ThrowUtils.throwIf(chatHistoryAdminQueryRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(chatHistoryService.listChatHistoryByPageForAdmin(chatHistoryAdminQueryRequest));
    }
}
