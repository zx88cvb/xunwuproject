package com.imocc.repository;

import java.util.List;

import com.imocc.entity.HouseTag;
import org.springframework.data.repository.CrudRepository;


/**
 * Created by 瓦力.
 */
public interface HouseTagRepository extends CrudRepository<HouseTag, Long> {
    HouseTag findByNameAndHouseId(String name, Long houseId);

    List<HouseTag> findAllByHouseId(Long id);

    List<HouseTag> findAllByHouseIdIn(List<Long> houseIds);
}
