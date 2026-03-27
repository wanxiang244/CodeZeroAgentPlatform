package com.yupi.yuaicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yupi.yuaicodemother.core.AiCodeGeneratorFacade;
import com.yupi.yuaicodemother.exception.BusinessException;
import com.yupi.yuaicodemother.exception.ErrorCode;
import com.yupi.yuaicodemother.exception.ThrowUtils;
import com.yupi.yuaicodemother.model.dto.AdminAppUpdateRequest;
import com.yupi.yuaicodemother.model.dto.AppQueryRequest;
import com.yupi.yuaicodemother.model.entity.App;
import com.yupi.yuaicodemother.mapper.AppMapper;
import com.yupi.yuaicodemother.model.enums.CodeGenTypeEnum;
import com.yupi.yuaicodemother.model.enums.UserRoleEnum;
import com.yupi.yuaicodemother.model.vo.AppVO;
import com.yupi.yuaicodemother.service.AppService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        return appVO;
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        return appList.stream()
                .map(this::getAppVO)
                .collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        Long userId = appQueryRequest.getUserId();
        String appName = appQueryRequest.getAppName();
        String codeGenType = appQueryRequest.getCodeGenType();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .eq("userId", userId)
                .like("appName", appName)
                .eq("codeGenType", codeGenType)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    @Override
    public boolean deleteApp(Long id, Long userId, String userRole) {
        // 参数校验
        if (id == null || userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }

        // 查询应用是否存在
        App app = this.getById(id);
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }

        // 权限校验：只能删除自己的应用或管理员可删除
        boolean isAdmin = UserRoleEnum.ADMIN.getValue().equals(userRole);
        boolean isOwner = app.getUserId().equals(userId);
        if (!isOwner && !isAdmin) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限删除该应用");
        }

        // 执行删除（逻辑删除）
        return this.removeById(id);
    }

    @Override
    public Page<AppVO> listMyAppByPage(long pageNum, long pageSize, Long userId) {
        // 限制每页最多 20 条
        pageSize = Math.min(pageSize, 20);

        // 构建查询条件
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("userId", userId)
                .orderBy("createTime", false);

        // 分页查询
        Page<App> appPage = this.page(Page.of(pageNum, pageSize), queryWrapper);

        // 转换为 AppVO
        Page<AppVO> resultPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        resultPage.setRecords(this.getAppVOList(appPage.getRecords()));

        return resultPage;
    }

    @Override
    public Page<AppVO> listFeaturedAppByPage(long pageNum, long pageSize, Long userId) {
        // 限制每页最多 20 条
        pageSize = Math.min(pageSize, 20);

        // 构建查询条件：优先级大于 0 的应用为精选应用，或者当前用户自己的应用
        QueryWrapper queryWrapper;
        if (userId != null) {
            // 有登录用户：精选应用 + 自己的应用
            queryWrapper = QueryWrapper.create()
                    .where("priority > ? OR userId = ?", 0, userId)
                    .orderBy("priority", false)
                    .orderBy("createTime", false);
        } else {
            // 未登录用户：仅精选应用
            queryWrapper = QueryWrapper.create()
                    .where("priority > ?", 0)
                    .orderBy("priority", false)
                    .orderBy("createTime", false);
        }

        // 分页查询
        Page<App> appPage = this.page(Page.of(pageNum, pageSize), queryWrapper);

        // 转换为 AppVO
        Page<AppVO> resultPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        resultPage.setRecords(this.getAppVOList(appPage.getRecords()));

        return resultPage;
    }

    @Override
    public boolean adminDeleteApp(Long id) {
        // 参数校验
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        }

        // 查询应用是否存在
        App app = this.getById(id);
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }

        // 执行删除（逻辑删除）
        return this.removeById(id);
    }

    @Override
    public boolean adminUpdateApp(AdminAppUpdateRequest adminAppUpdateRequest) {
        // 参数校验
        if (adminAppUpdateRequest == null || adminAppUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }

        // 查询应用是否存在
        Long id = adminAppUpdateRequest.getId();
        App oldApp = this.getById(id);
        if (oldApp == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }

        // 构建更新实体
        App app = new App();
        app.setId(id);
        app.setAppName(adminAppUpdateRequest.getAppName());
        app.setCover(adminAppUpdateRequest.getCover());
        app.setPriority(adminAppUpdateRequest.getPriority());
        // 设置编辑时间
        app.setEditTime(LocalDateTime.now());

        boolean result = this.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return true;
    }

    @Override
    public Flux<String> chatToGenCode(Long appId, Long userId) {
        // 参数校验
        if (appId == null || appId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用ID无效");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户未登录");
        }

        // 查询应用是否存在
        App app = this.getById(appId);
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }

        // 权限校验：只能操作自己的应用
        if (!app.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限操作该应用");
        }

        // 获取初始提示词
        String initPrompt = app.getInitPrompt();
        if (StrUtil.isBlank(initPrompt)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用初始化提示词为空");
        }

        // 获取代码生成类型
        String codeGenTypeValue = app.getCodeGenType();
        CodeGenTypeEnum codeGenType = CodeGenTypeEnum.getEnumByValue(codeGenTypeValue);
        if (codeGenType == null) {
            codeGenType = CodeGenTypeEnum.MULTI_FILE; // 默认多文件生成
        }

        // 调用 AI 代码生成门面，流式生成代码
        return aiCodeGeneratorFacade.generateAndSaveCodeStream(initPrompt, codeGenType, appId);
    }

    @Override
    public boolean existsByDeployKey(String deployKey) {
        if (StrUtil.isBlank(deployKey)) {
            return false;
        }
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("deployKey", deployKey);
        return this.exists(queryWrapper);
    }


}