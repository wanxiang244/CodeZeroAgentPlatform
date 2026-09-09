package com.ljj.CodeZeroAgentPlatform.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用创建请求
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 */
@Data
public class AppAddRequest implements Serializable {

    /**
     * 初始化提示词
     */
    private String initPrompt;

    private static final long serialVersionUID = 1L;
}