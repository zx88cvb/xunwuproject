package com.imocc.service.search;

import lombok.Data;

/**
 * 自动补全实体
 * @Author: Angel
 * @Date: 2019/6/21.
 * @Description:
 */
@Data
public class HouseSuggest {
    /**
     * 输入的字符串
     */
    private String input;

    /**
     * 权重 默认为10
     */
    private int weight = 10;
}
