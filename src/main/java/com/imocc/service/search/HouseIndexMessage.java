package com.imocc.service.search;

import lombok.Data;

/**
 * @Author: Angel
 * @Date: 2019/6/20.
 * @Description:
 */
@Data
public class HouseIndexMessage {

    public static final String INDEX = "index";
    public static final String REMOVE = "remove";
    public static final int MAX_RETRY = 3;

    private Long houseId;

    private String operation;

    private int retry = 0;

    public HouseIndexMessage() {
    }

    public HouseIndexMessage(Long houseId, String operation, int retry) {
        this.houseId = houseId;
        this.operation = operation;
        this.retry = retry;
    }
}
