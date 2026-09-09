package com.ljj.CodeZeroAgentPlatform.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.ljj.CodeZeroAgentPlatform.constant.AppConstant;
import com.ljj.CodeZeroAgentPlatform.core.builder.VueProjectBuilder;
import com.ljj.CodeZeroAgentPlatform.exception.BusinessException;
import com.ljj.CodeZeroAgentPlatform.exception.ErrorCode;
import com.ljj.CodeZeroAgentPlatform.model.entity.App;
import com.ljj.CodeZeroAgentPlatform.model.enums.CodeGenTypeEnum;
import com.ljj.CodeZeroAgentPlatform.service.AppService;
import com.ljj.CodeZeroAgentPlatform.service.DeployService;
import com.ljj.CodeZeroAgentPlatform.service.ScreenshotService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 应用部署服务实现类
 * 支持 HTML、多文件和 Vue 项目三种类型的部署
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 */
@Slf4j
@Service
public class DeployServiceImpl implements DeployService {

    @Resource
    private AppService appService;

    @Resource
    private ScreenshotService screenshotService;

    /**
     * Vue 项目构建器，用于构建 Vue 项目并获取 dist 目录
     */
    @Resource
    private VueProjectBuilder vueProjectBuilder;

    /**
     * 虚拟线程执行器，用于异步执行截图任务
     */
    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    // 随机字符串字符集（大小写字母+数字）
    private static final String BASE_CHAR = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    @Override
    public String deployApp(Long appId, Long userId) {
        // 参数校验
        if (appId == null || appId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用ID无效");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户未登录");
        }

        // 查询应用是否存在
        App app = appService.getById(appId);
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }

        // 权限校验：只能部署自己的应用
        if (!app.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限部署该应用");
        }

        // 获取或生成deployKey
        String deployKey = app.getDeployKey();
        boolean isNewDeployKey = false;
        if (StrUtil.isBlank(deployKey)) {
            // 生成唯一的deployKey
            deployKey = generateUniqueDeployKey();
            isNewDeployKey = true;
        }

        // 获取代码生成类型
        String codeGenType = app.getCodeGenType();
        if (StrUtil.isBlank(codeGenType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用代码生成类型为空");
        }

        // 源目录路径（根据项目类型动态确定）
        String sourceDirPath;

        // 判断是否为 Vue 项目类型
        boolean isVueProject = CodeGenTypeEnum.VUE_PROJECT.getValue().equals(codeGenType);

        // 如果是 Vue 项目，先执行构建
        if (isVueProject) {
            log.info("检测到 Vue 项目，开始执行构建，appId: {}", appId);
            boolean buildSuccess = vueProjectBuilder.buildVueProjectSync(appId);
            if (!buildSuccess) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "Vue 项目构建失败，无法部署");
            }
            // Vue 项目使用 dist 目录作为源目录
            File distDir = vueProjectBuilder.getDistDir(appId);
            if (!distDir.exists() || !distDir.isDirectory()) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "Vue 项目构建产物不存在，请先生成代码");
            }
            sourceDirPath = distDir.getAbsolutePath();
            log.info("Vue 项目构建成功，使用 dist 目录作为部署源：{}", sourceDirPath);
        } else {
            // 源目录路径（非 Vue 项目）
            sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + "/" + codeGenType + "_" + appId;
        }

        // 检查源目录是否存在
        if (!FileUtil.exist(sourceDirPath)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用代码文件不存在，请先生成代码");
        }

        // 目标目录路径
        String targetDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + "/" + deployKey;

        try {
            // 清理并重新创建目标目录，确保干净的部署
            if (FileUtil.exist(targetDirPath)) {
                FileUtil.del(targetDirPath);
            }
            FileUtil.mkdir(targetDirPath);

            // 检查源目录是否存在文件
            List<String> fileNames = FileUtil.listFileNames(sourceDirPath);
            if (fileNames == null || fileNames.isEmpty()) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用代码文件为空，请先生成代码");
            }

            FileUtil.copyContent(new File(sourceDirPath), new File(targetDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "部署失败：" + e.getMessage());
        }

        // 如果是新生成的deployKey，或者需要更新部署时间，更新应用信息
        if (isNewDeployKey) {
            App updateApp = new App();
            updateApp.setId(appId);
            updateApp.setDeployKey(deployKey);
            updateApp.setDeployedTime(LocalDateTime.now());
            boolean result = appService.updateById(updateApp);
            if (!result) {
                // 如果更新失败，清理已复制的文件
                FileUtil.del(targetDirPath);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "部署失败：更新应用信息失败");
            }
        } else {
            // 已有deployKey，只更新部署时间
            App updateApp = new App();
            updateApp.setId(appId);
            updateApp.setDeployedTime(LocalDateTime.now());
            appService.updateById(updateApp);
        }

        // 返回部署URL
        String resultUrl = StrUtil.removeSuffix(AppConstant.APP_DEPLOY_DOMAIN, "/") + "/" + deployKey;

        // 异步生成截图（不阻塞部署流程）
        final Long finalAppId = appId;
        final String finalDeployUrl = resultUrl;
        virtualThreadExecutor.submit(() -> {
            try {
                String coverUrl = screenshotService.generateAndUploadScreenshot(finalDeployUrl);
                if (StrUtil.isNotBlank(coverUrl)) {
                    // 更新应用的 cover 字段
                    App updateApp = new App();
                    updateApp.setId(finalAppId);
                    updateApp.setCover(coverUrl);
                    appService.updateById(updateApp);
                    log.info("应用 {} 截图生成成功: {}", finalAppId, coverUrl);
                }
            } catch (Exception e) {
                log.error("应用 {} 截图生成失败: {}", finalAppId, e.getMessage());
            }
        });

        return resultUrl;
    }

    /**
     * 生成唯一的deployKey
     *
     * @return 唯一的6位随机字符串
     */
    private String generateUniqueDeployKey() {
        String deployKey;
        int maxAttempts = 10; // 最大尝试次数，防止无限循环
        int attempts = 0;

        do {
            if (attempts >= maxAttempts) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "生成deployKey失败：重试次数过多");
            }
            // 生成6位随机字符串
            deployKey = RandomUtil.randomString(BASE_CHAR, 6);
            attempts++;
        } while (appService.existsByDeployKey(deployKey));

        return deployKey;
    }
}
