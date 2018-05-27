package com.imocc.repository;

import com.imocc.entity.User;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by Administrator on 2018/1/4.
 */
public interface UserRepository extends CrudRepository<User,Long> {
    User findUserByName(String userName);
}
