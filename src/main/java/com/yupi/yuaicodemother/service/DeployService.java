package com.yupi.yuaicodemother.service;

/**
 * 应用部署服务接口
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 */
public interface DeployService {

    /**
     * 部署应用
     *
     * @param appId  应用ID
     * @param userId 用户ID（用于权限验证）
     * @return 部署URL
     */
    String deployApp(Long appId, Long userId);
}