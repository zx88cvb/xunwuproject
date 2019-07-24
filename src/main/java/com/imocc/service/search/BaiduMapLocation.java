package com.imocc.service.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 百度地图
 * @Author: Angel
 * @Date: 2019/6/25.
 * @Description:
 */
@Data
public class BaiduMapLocation {
    /**
     * 经度
     */
    @JsonProperty("lon")
    private double longitude;

    /**
     * 纬度
     */
    @JsonProperty("lat")
    private double latitude;
}
