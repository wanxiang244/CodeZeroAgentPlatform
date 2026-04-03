package com.yupi.yuaicodemother.controller;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.yupi.yuaicodemother.annotation.AuthCheck;
import com.yupi.yuaicodemother.common.BaseResponse;
import com.yupi.yuaicodemother.common.ResultUtils;
import com.yupi.yuaicodemother.constant.UserConstant;
import com.yupi.yuaicodemother.exception.BusinessException;
import com.yupi.yuaicodemother.exception.ErrorCode;
import com.yupi.yuaicodemother.exception.ThrowUtils;
import com.yupi.yuaicodemother.model.dto.AdminAppUpdateRequest;
import com.yupi.yuaicodemother.model.dto.AppAddRequest;
import com.yupi.yuaicodemother.model.dto.AppDeleteRequest;
import com.yupi.yuaicodemother.model.dto.AppQueryRequest;
import com.yupi.yuaicodemother.model.dto.AppUpdateRequest;
import com.yupi.yuaicodemother.model.entity.App;
import com.yupi.yuaicodemother.model.entity.User;
import com.yupi.yuaicodemother.model.enums.CodeGenTypeEnum;
import com.yupi.yuaicodemother.model.vo.AppVO;
import com.yupi.yuaicodemother.service.AppService;
import com.yupi.yuaicodemother.service.DeployService;
import com.yupi.yuaicodemother.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Flux;

/**
 * 应用 控制层。
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 */
@RestController
@RequestMapping("/app")
public class AppController {

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    @Resource
    private DeployService deployService;

    // ==================== 用户端接口 ====================

    /**
     * 创建应用
     *
     * @param appAddRequest 应用创建请求
     * @param request       HTTP 请求
     * @return 新应用 id
     */
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);

        // 获取初始化提示词
        String initPrompt = appAddRequest.getInitPrompt();
        if (StrUtil.isBlank(initPrompt)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "初始化提示词不能为空");
        }

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 创建应用实体
        App app = new App();
        // 应用名称：取提示词前12位
        String appName = initPrompt.length() > 12 ? initPrompt.substring(0, 12) : initPrompt;
        app.setAppName(appName);
        app.setInitPrompt(initPrompt);
        // 默认代码生成类型：多文件生成
        app.setCodeGenType(CodeGenTypeEnum.MULTI_FILE.getValue());
        app.setUserId(loginUser.getId());

        // 保存应用
        boolean result = appService.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        return ResultUtils.success(app.getId());
    }

    /**
     * 更新应用（仅更新应用名称）
     *
     * @param appUpdateRequest 应用更新请求
     * @param request          HTTP 请求
     * @return 更新结果
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateRequest appUpdateRequest, HttpServletRequest request) {
        if (appUpdateRequest == null || appUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 查询应用是否存在
        Long id = appUpdateRequest.getId();
        App oldApp = appService.getById(id);
        if (oldApp == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }

        // 权限校验：只能修改自己的应用
        if (!oldApp.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限修改该应用");
        }

        // 更新应用名称
        App app = new App();
        app.setId(id);
        app.setAppName(appUpdateRequest.getAppName());

        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        return ResultUtils.success(true);
    }

    /**
     * 删除应用
     *
     * @param appDeleteRequest 应用删除请求
     * @param request          HTTP 请求
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteApp(@RequestBody AppDeleteRequest appDeleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appDeleteRequest == null, ErrorCode.PARAMS_ERROR);

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 删除应用（包含权限校验：本人或管理员可删除）
        boolean result = appService.deleteApp(appDeleteRequest.getId(), loginUser.getId(), loginUser.getUserRole());
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        return ResultUtils.success(true);
    }

    /**
     * 查看应用详情（用户只能查看自己的应用）
     *
     * @param id      应用 id
     * @param request HTTP 请求
     * @return 应用详情
     */
    @GetMapping("/get/vo")
    public BaseResponse<AppVO> getAppVOById(@RequestParam Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);

        // 查询应用是否存在
        App app = appService.getById(id);
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 权限校验：只能查看自己的应用
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限查看该应用");
        }

        // 返回应用详情
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 分页查询用户自己的应用
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param request  HTTP 请求
     * @return 应用分页列表
     */
    @GetMapping("/my/list/page")
    public BaseResponse<Page<AppVO>> listMyAppByPage(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            HttpServletRequest request) {
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 分页查询
        Page<AppVO> resultPage = appService.listMyAppByPage(pageNum, pageSize, loginUser.getId());
        return ResultUtils.success(resultPage);
    }

    /**
     * 分页查询精选应用（包括自己的）
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param request  HTTP 请求
     * @return 精选应用分页列表
     */
    @GetMapping("/featured/list/page")
    public BaseResponse<Page<AppVO>> listFeaturedAppByPage(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            HttpServletRequest request) {
        // 获取当前登录用户（可为空，允许未登录用户查看精选应用）
        User loginUser = null;
        try {
            loginUser = userService.getLoginUser(request);
        } catch (Exception e) {
            // 未登录用户，loginUser 保持为 null
        }

        Long userId = loginUser != null ? loginUser.getId() : null;

        // 分页查询
        Page<AppVO> resultPage = appService.listFeaturedAppByPage(pageNum, pageSize, userId);
        return ResultUtils.success(resultPage);
    }

    /**
     * 部署应用
     *
     * @param appId   应用 id
     * @param request HTTP 请求
     * @return 部署URL
     */
    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(@RequestParam Long appId, HttpServletRequest request) {
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 调用部署服务
        String deployUrl = deployService.deployApp(appId, loginUser.getId());

        return ResultUtils.success(deployUrl);
    }

    // ==================== 管理员接口 ====================

    /**
     * 分页获取应用列表（仅管理员）
     *
     * @param appQueryRequest 查询请求参数
     * @return 分页结果
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AppVO>> listAppVOByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = appQueryRequest.getPageNum();
        long pageSize = appQueryRequest.getPageSize();
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize),
                appService.getQueryWrapper(appQueryRequest));
        // 数据脱敏
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService. getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 根据 id 获取应用详情（仅管理员）
     *
     * @param id 应用 id
     * @return 应用详情
     */
    @GetMapping("admin/get/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AppVO> getAppVOByIdByAdmin(@RequestParam long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 管理员删除应用
     *
     * @param appDeleteRequest 应用删除请求
     * @param request          HTTP 请求
     * @return 是否删除成功
     */
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> adminDeleteApp(@RequestBody AppDeleteRequest appDeleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appDeleteRequest == null, ErrorCode.PARAMS_ERROR);

        // 管理员删除应用
        boolean result = appService.adminDeleteApp(appDeleteRequest.getId());
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        return ResultUtils.success(true);
    }

    /**
     * 管理员更新应用
     *
     * @param adminAppUpdateRequest 管理员应用更新请求
     * @param request               HTTP 请求
     * @return 更新结果
     */
    @PostMapping("/admin/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> adminUpdateApp(@RequestBody AdminAppUpdateRequest adminAppUpdateRequest, HttpServletRequest request) {
        if (adminAppUpdateRequest == null || adminAppUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 管理员更新应用
        boolean result = appService.adminUpdateApp(adminAppUpdateRequest);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        return ResultUtils.success(true);
    }

    /**
     * 对话生成代码（流式）
     * 根据应用 id 获取应用信息，使用初始提示词生成代码
     *
     * @param appId   应用 id
     * @param request HTTP 请求
     * @return 流式代码生成响应
     */
    @GetMapping(value = "/chat-to-gen-code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatToGenCode(@RequestParam Long appId, HttpServletRequest request) {
        // 权限校验：确保用户已登录
        User loginUser = userService.getLoginUser(request);

        // 查询应用是否存在
        App app = appService.getById(appId);
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }

        // 权限校验：只能操作自己的应用
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限操作该应用");
        }

        // 调用服务层流式生成代码
        return appService.chatToGenCode(appId, loginUser.getId());
    }
}