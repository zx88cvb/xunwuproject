package com.imocc.repository;

import com.imocc.entity.House;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

/**
 * Created by Administrator on 2018/1/11.
 */
public interface HouseRepository extends PagingAndSortingRepository<House,Long>,
        JpaSpecificationExecutor<House>{
    @Modifying
    @Query("UPDATE House as house set house.cover = :cover where house.id = :id")
    void updateCover(@Param("id") Long targetId,@Param("cover") String path);

    @Modifying
    @Query("UPDATE House as house set house.status = :status where house.id=:id")
    void updateStatus(@Param("id") Long id,@Param("status") int status);
}
