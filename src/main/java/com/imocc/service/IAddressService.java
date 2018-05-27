package com.imocc.service;

import com.imocc.entity.SupportAddress;
import com.imocc.web.controller.house.SubwayDTO;
import com.imocc.web.controller.house.SubwayStationDTO;
import com.imocc.web.controller.house.SupportAddressDTO;

import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2018/1/11.
 *
 * 地址服务接口
 */
public interface IAddressService {
    ServiceMultiResult<SupportAddressDTO> findAllCities();

    ServiceResult<SubwayStationDTO> findSubwayStation(Long subwayStationId);

    ServiceResult<SubwayDTO> findSubway(Long subwayLineId);

    Map<SupportAddress.Level,SupportAddressDTO> findCityAndRegion(String cityEnName, String regionEnName);

    /**
     * 根据城市英文简写获取该城市所有支持的区域信息
     * @param cityName
     * @return
     */
    ServiceMultiResult findAllRegionsByCityName(String cityName);

    /**
     * 获取该城市所有的地铁线路
     * @param cityEnName
     * @return
     */
    List<SubwayDTO> findAllSubwayByCity(String cityEnName);

    /**
     * 获取地铁线路所有的站点
     * @param subwayId
     * @return
     */
    List<SubwayStationDTO> findAllStationBySubway(Long subwayId);

    /**
     * 根据英文简写获取城市详细信息
     * @param cityEnName
     * @return
     */
    ServiceResult<SupportAddressDTO> findCity(String cityEnName);
}
