package com.imocc.service;

import com.imocc.entity.User;
import com.imocc.web.controller.house.UserDTO;

/**
 * Created by Administrator on 2018/1/6.
 */
public interface IUserService {
    User findUserByName(String userName);

    /**
     * 根据id查询
     * @param adminId
     * @return
     */
    ServiceResult<UserDTO> findById(Long adminId);
}
