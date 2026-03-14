package com.yupi.yuaicodemother.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用查询请求
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 */
@Data
public class AppQueryRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 代码生成类型
     */
    private String codeGenType;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（ascend, descend）
     */
    private String sortOrder;

    /**
     * 当前页号
     */
    private long pageNum = 1;

    /**
     * 页面大小
     */
    private long pageSize = 10;

    private static final long serialVersionUID = 1L;
}