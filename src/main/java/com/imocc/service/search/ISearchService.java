package com.imocc.service.search;

import com.imocc.service.ServiceMultiResult;
import com.imocc.web.controller.form.RentSearch;

/**
 * Created by Administrator on 2018/1/14.
 * 检索接口
 */
public interface ISearchService {

    /**
     * 索引目标房源
     * @param houseId
     */
    void index(Long houseId);

    /**
     * 移除房源索引
     * @param houseId
     */
    void remove(Long houseId);

    /**
     * 查询租房接口
     * @param rentSearch 租房请求参数结构体
     * @return id
     */
    ServiceMultiResult<Long> query(RentSearch rentSearch);
}
