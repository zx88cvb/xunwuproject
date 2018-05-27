package com.imocc.service;

import com.imocc.entity.House;
import com.imocc.web.controller.form.DatatableSearch;
import com.imocc.web.controller.form.HouseForm;
import com.imocc.web.controller.form.RentSearch;
import com.imocc.web.controller.house.HouseDTO;

/**
 * Created by Administrator on 2018/1/11.
 * 房屋管理接口
 */
public interface IHouseService {
    ServiceResult<HouseDTO> save(HouseForm houseForm);

    /**
     * 房源列表查询 （管理员）
     * @param searchBody
     * @return
     */
    ServiceMultiResult<HouseDTO> adminQuery(DatatableSearch searchBody);

    /**
     * 根据id查询
     * @param id
     * @return
     */
    ServiceResult<HouseDTO> findCompleteOne(Long id);

    /**
     * 修改
     * @param houseForm
     * @return
     */
    ServiceResult update(HouseForm houseForm);

    /**
     * 修改封面
     * @param coverId
     * @param targetId
     * @return
     */
    ServiceResult updateCover(Long coverId, Long targetId);

    /**
     * 移除图片
     * @param id
     * @return
     */
    ServiceResult removePhoto(Long id);

    /**
     * 新增标签
     * @param houseId
     * @param tag
     * @return
     */
    ServiceResult addTag(Long houseId, String tag);

    /**
     * 移除标签
     * @param houseId
     * @param tag
     * @return
     */
    ServiceResult removeTag(Long houseId, String tag);

    /**
     * 审核房源
     * @param id
     * @param value
     * @return
     */
    ServiceResult updateStatus(Long id, int value);

    /**
     * 查询房源信息
     * @param rentSearch
     * @return
     */
    ServiceMultiResult<HouseDTO> query(RentSearch rentSearch);
}
