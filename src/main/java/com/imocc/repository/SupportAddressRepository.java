package com.imocc.repository;

import com.imocc.entity.SupportAddress;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * Created by Administrator on 2018/1/11.
 */
public interface SupportAddressRepository extends CrudRepository<SupportAddress,Long> {

    /**
     * 获取所有对应行政级别的信息
     * @param level
     * @return
     */
    List<SupportAddress> findAllByLevel(String level);

    SupportAddress findByEnNameAndLevel(String cityEnName, String value);

    SupportAddress findByEnNameAndBelongTo(String regionEnName, String enName);

    List<SupportAddress> findAllByLevelAndBelongTo(String value, String cityName);
}
