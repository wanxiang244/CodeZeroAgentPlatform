package com.yupi.yuaicodemother.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.yupi.yuaicodemother.exception.BusinessException;
import com.yupi.yuaicodemother.exception.ErrorCode;
import com.yupi.yuaicodemother.model.entity.App;
import com.yupi.yuaicodemother.service.AppService;
import com.yupi.yuaicodemother.service.DeployService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 应用部署服务实现类
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 */
@Service
public class DeployServiceImpl implements DeployService {

    @Resource
    private AppService appService;

    // 部署域名
    private static final String DEPLOY_DOMAIN = "http://localhost";

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

        // 检查是否已经部署过
        if (StrUtil.isNotBlank(app.getDeployKey())) {
            // 已经部署过，直接返回已有的URL
            return DEPLOY_DOMAIN + "/" + app.getDeployKey();
        }

        // 获取代码生成类型
        String codeGenType = app.getCodeGenType();
        if (StrUtil.isBlank(codeGenType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用代码生成类型为空");
        }

        // 源目录路径
        String sourceDirPath = System.getProperty("user.dir") + "/tmp/code_output/" + codeGenType + "_" + appId;
        // 检查源目录是否存在
        if (!FileUtil.exist(sourceDirPath)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用代码文件不存在，请先生成代码");
        }

        // 生成唯一的deployKey
        String deployKey = generateUniqueDeployKey();

        // 目标目录路径
        String targetDirPath = System.getProperty("user.dir") + "/tmp/code_deploy/" + deployKey;

        try {
            // 创建目标目录
            FileUtil.mkdir(targetDirPath);

            // 获取源目录下的所有文件名
            List<String> fileNames = FileUtil.listFileNames(sourceDirPath);
            if (fileNames == null || fileNames.isEmpty()) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用代码文件为空，请先生成代码");
            }

            // 将源目录下的所有文件复制到目标目录
            for (String fileName : fileNames) {
                String sourceFilePath = sourceDirPath + "/" + fileName;
                String targetFilePath = targetDirPath + "/" + fileName;
                FileUtil.copy(sourceFilePath, targetFilePath, true);
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "部署失败：" + e.getMessage());
        }

        // 更新应用的deployKey和部署时间
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

        // 返回部署URL
        return DEPLOY_DOMAIN + "/" + deployKey;
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