package com.yupi.yuaicodemother.controller;

import cn.hutool.core.util.StrUtil;
import com.yupi.yuaicodemother.common.BaseResponse;
import com.yupi.yuaicodemother.common.ResultUtils;
import com.yupi.yuaicodemother.exception.BusinessException;
import com.yupi.yuaicodemother.exception.ErrorCode;
import com.yupi.yuaicodemother.exception.ThrowUtils;
import com.yupi.yuaicodemother.model.dto.AppAddRequest;
import com.yupi.yuaicodemother.model.dto.AppUpdateRequest;
import com.yupi.yuaicodemother.model.entity.App;
import com.yupi.yuaicodemother.model.entity.User;
import com.yupi.yuaicodemother.model.enums.CodeGenTypeEnum;
import com.yupi.yuaicodemother.service.AppService;
import com.yupi.yuaicodemother.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}