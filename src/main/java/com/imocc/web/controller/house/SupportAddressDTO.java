package com.imocc.web.controller.house;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Created by Administrator on 2018/1/11.
 */
@Data
public class SupportAddressDTO {
    private Long id;

    @JsonProperty(value = "belong_to")
    private String belongTo;

    @JsonProperty(value = "en_name")
    private String enName;

    @JsonProperty(value = "cn_name")
    private String cnName;

    private String level;

    private double baiduMapLongitude;

    private double baiduMapLatitude;
}
