package com.imocc.service.search;

import lombok.Data;

/**
 * @Author: Angel
 * @Date: 2019/6/24.
 * @Description:
 */
@Data
public class HouseBucketDTO {
    /**
     * bucket key
     */
    private String key;

    /**
     * 聚合结果值
     */
    private Long count;

    public HouseBucketDTO(String key, Long count) {
        this.key = key;
        this.count = count;
    }
}
