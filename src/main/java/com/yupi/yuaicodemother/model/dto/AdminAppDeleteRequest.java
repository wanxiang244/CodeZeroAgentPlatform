package com.yupi.yuaicodemother.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理员应用删除请求
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 */
@Data
public class AdminAppDeleteRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}