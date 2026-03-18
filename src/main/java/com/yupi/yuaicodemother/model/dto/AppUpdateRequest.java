package com.yupi.yuaicodemother.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用更新请求
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 */
@Data
public class AppUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    private static final long serialVersionUID = 1L;
}