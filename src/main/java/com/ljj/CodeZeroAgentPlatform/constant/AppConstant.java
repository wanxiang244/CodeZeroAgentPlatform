package com.ljj.CodeZeroAgentPlatform.constant;

/**
 * 应用常量
 */
public interface AppConstant {

    /**
     * 应用生成根目录
     */
    String CODE_OUTPUT_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * 应用部署根目录
     */
    String CODE_DEPLOY_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_deploy";

    /**
     * 应用部署域名
     */
    String APP_DEPLOY_DOMAIN = "http://localhost";
}
