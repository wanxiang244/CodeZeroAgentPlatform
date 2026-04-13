package com.yupi.yuaicodemother.core.builder;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Vue 项目构建器
 * 用于执行 Vue 项目的依赖安装和打包构建
 * 支持 Windows 和 Linux/Mac 系统的命令适配
 *
 * @author yupi
 */
@Slf4j
@Component
public class VueProjectBuilder {

    /**
     * npm 安装依赖的命令
     */
    private static final String NPM_INSTALL_COMMAND = "npm install";

    /**
     * npm 打包构建的命令
     */
    private static final String NPM_BUILD_COMMAND = "npm run build";

    /**
     * 构建成功的输出标识
     * 不同构建工具的成功标识可能不同，这里列举常见的几种
     */
    private static final String[] BUILD_SUCCESS_MARKERS = {
            "Build completed",
            "build complete",
            "Done in",
            "Successfully",
            "✓ built in"
    };

    /**
     * 执行任意命令的通用方法
     * 通过 Java 的 ProcessBuilder 执行命令，实时读取输出日志
     *
     * @param command    要执行的命令
     * @param workingDir 命令执行的工作目录
     * @return 命令执行是否成功（true：成功，false：失败）
     */
    public boolean executeCommand(String command, File workingDir) {
        if (StrUtil.isBlank(command)) {
            log.warn("命令为空，跳过执行");
            return false;
        }

        // 根据操作系统适配命令
        String[] commands = adaptCommandForOS(command);
        log.info("开始执行命令: {}, 工作目录: {}", command, workingDir.getAbsolutePath());

        try {
            // 使用 ProcessBuilder 执行命令
            ProcessBuilder processBuilder = new ProcessBuilder(commands);
            // 设置工作目录
            if (workingDir != null && workingDir.exists()) {
                processBuilder.directory(workingDir);
            }
            // 合合错误流和标准输出流，便于统一读取
            processBuilder.redirectErrorStream(true);

            // 启动进程
            Process process = processBuilder.start();

            // 实时读取输出日志
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[构建日志] {}", line);
                }
            }

            // 等待进程完成并获取退出码
            int exitCode = process.waitFor();
            boolean success = exitCode == 0;
            if (success) {
                log.info("命令执行成功: {}", command);
            } else {
                log.error("命令执行失败，退出码: {}, 命令: {}", exitCode, command);
            }
            return success;
        } catch (Exception e) {
            log.error("执行命令时发生异常: {}", command, e);
            return false;
        }
    }

    /**
     * 根据操作系统适配命令
     * Windows 系统需要添加 cmd /c 前缀，且 npm 命令需要使用 .cmd 扩展名
     *
     * @param command 原始命令
     * @return 适配后的命令数组
     */
    private String[] adaptCommandForOS(String command) {
        // Windows 系统特殊处理
        if (isWindows()) {
            // 将 npm 命令替换为 npm.cmd（Windows 上 npm 实际是 npm.cmd）
            String adaptedCommand = command.replace("npm", "npm.cmd");
            // 使用 cmd /c 执行命令
            return new String[]{"cmd", "/c", adaptedCommand};
        }
        // Linux/Mac 系统直接执行
        return new String[]{"sh", "-c", command};
    }

    /**
     * 判断当前系统是否为 Windows
     *
     * @return true：Windows 系统，false：其他系统
     */
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    /**
     * 执行安装依赖命令
     * 在 Vue 项目根目录执行 npm install
     *
     * @param projectDir Vue 项目根目录
     * @return 安装是否成功
     */
    public boolean installDependencies(File projectDir) {
        if (!validateProjectDir(projectDir)) {
            return false;
        }
        log.info("开始安装 Vue 项目依赖，目录: {}", projectDir.getAbsolutePath());
        return executeCommand(NPM_INSTALL_COMMAND, projectDir);
    }

    /**
     * 执行打包构建命令
     * 在 Vue 项目根目录执行 npm run build
     *
     * @param projectDir Vue 项目根目录
     * @return 构建是否成功
     */
    public boolean buildProject(File projectDir) {
        if (!validateProjectDir(projectDir)) {
            return false;
        }
        log.info("开始打包构建 Vue 项目，目录: {}", projectDir.getAbsolutePath());
        boolean success = executeCommand(NPM_BUILD_COMMAND, projectDir);

        // 验证构建产物是否生成
        if (success) {
            success = validateBuildOutput(projectDir);
        }
        return success;
    }

    /**
     * 验证项目目录是否有效
     * 检查目录是否存在且包含 package.json 文件
     *
     * @param projectDir 项目目录
     * @return true：有效，false：无效
     */
    private boolean validateProjectDir(File projectDir) {
        if (projectDir == null || !projectDir.exists()) {
            log.warn("项目目录不存在: {}", projectDir);
            return false;
        }
        // 检查是否存在 package.json 文件（Vue 项目必备）
        File packageJson = new File(projectDir, "package.json");
        if (!packageJson.exists()) {
            log.warn("项目目录缺少 package.json 文件，可能不是有效的 Vue 项目: {}", projectDir.getAbsolutePath());
            return false;
        }
        return true;
    }

    /**
     * 验证构建产物是否生成
     * 检查 dist 目录是否存在且包含 index.html 文件
     *
     * @param projectDir 项目目录
     * @return true：构建产物有效，false：构建产物无效
     */
    private boolean validateBuildOutput(File projectDir) {
        // Vue 项目构建后默认生成 dist 目录
        File distDir = new File(projectDir, "dist");
        if (!distDir.exists() || !distDir.isDirectory()) {
            log.warn("构建产物目录 dist 不存在: {}", projectDir.getAbsolutePath());
            return false;
        }
        // 检查是否存在 index.html（构建后的入口文件）
        File indexHtml = new File(distDir, "index.html");
        if (!indexHtml.exists()) {
            log.warn("构建产物缺少 index.html 文件: {}", distDir.getAbsolutePath());
            return false;
        }
        log.info("构建产物验证成功，dist 目录: {}", distDir.getAbsolutePath());
        return true;
    }

    /**
     * 构建 Vue 项目（完整流程）
     * 依次执行：安装依赖 -> 打包构建 -> 验证产物
     * 使用 Java 21 虚拟线程在后台异步执行，不阻塞主业务流程
     *
     * 注意：该方法不会抛出异常，即使构建失败也只会记录日志，
     * 防止影响主业务流程（代码生成）
     *
     * @param appId 应用 ID，用于定位项目目录
     */
    public void buildVueProject(Long appId) {
        if (appId == null) {
            log.warn("appId 为空，无法执行构建");
            return;
        }

        // 使用 Java 21 虚拟线程异步执行构建，不阻塞主线程
        Thread.ofVirtual().start(() -> {
            try {
                // 获取项目目录路径
                File projectDir = getProjectDir(appId);
                log.info("开始构建 Vue 项目，appId: {}, 目录: {}", appId, projectDir.getAbsolutePath());

                // 执行安装依赖
                boolean installSuccess = installDependencies(projectDir);
                if (!installSuccess) {
                    log.error("Vue 项目依赖安装失败，appId: {}", appId);
                    return;
                }

                // 执行打包构建
                boolean buildSuccess = buildProject(projectDir);
                if (!buildSuccess) {
                    log.error("Vue 项目打包构建失败，appId: {}", appId);
                    return;
                }

                log.info("Vue 项目构建完成，appId: {}, 构建产物目录: {}",
                        appId, new File(projectDir, "dist").getAbsolutePath());
            } catch (Exception e) {
                // 捕获所有异常，防止影响主业务流程
                log.error("Vue 项目构建过程发生异常，appId: {}", appId, e);
            }
        });
    }

    /**
     * 构建 Vue 项目（完整流程，同步执行）
     * 依次执行：安装依赖 -> 打包构建 -> 验证产物
     * 该方法会阻塞调用线程，适用于部署场景
     *
     * @param appId 应用 ID，用于定位项目目录
     * @return 构建是否成功
     */
    public boolean buildVueProjectSync(Long appId) {
        if (appId == null) {
            log.warn("appId 为空，无法执行构建");
            return false;
        }

        try {
            // 获取项目目录路径
            File projectDir = getProjectDir(appId);
            log.info("开始同步构建 Vue 项目，appId: {}, 目录: {}", appId, projectDir.getAbsolutePath());

            // 执行安装依赖
            boolean installSuccess = installDependencies(projectDir);
            if (!installSuccess) {
                log.error("Vue 项目依赖安装失败，appId: {}", appId);
                return false;
            }

            // 执行打包构建
            boolean buildSuccess = buildProject(projectDir);
            if (!buildSuccess) {
                log.error("Vue 项目打包构建失败，appId: {}", appId);
                return false;
            }

            log.info("Vue 项目同步构建完成，appId: {}, 构建产物目录: {}",
                    appId, new File(projectDir, "dist").getAbsolutePath());
            return true;
        } catch (Exception e) {
            log.error("Vue 项目同步构建过程发生异常，appId: {}", appId, e);
            return false;
        }
    }

    /**
     * 根据 appId 获取 Vue 项目目录
     * Vue 项目保存在 CODE_OUTPUT_ROOT_DIR/vue_project_{appId} 目录下
     *
     * @param appId 应用 ID
     * @return 项目目录
     */
    public File getProjectDir(Long appId) {
        String projectDirPath = System.getProperty("user.dir") + "/tmp/code_output/vue_project_" + appId;
        return new File(projectDirPath);
    }

    /**
     * 根据 appId 获取 Vue 项目构建后的 dist 目录
     * dist 目录位于项目目录下，包含打包后的静态文件
     *
     * @param appId 应用 ID
     * @return dist 目录
     */
    public File getDistDir(Long appId) {
        File projectDir = getProjectDir(appId);
        return new File(projectDir, "dist");
    }
}