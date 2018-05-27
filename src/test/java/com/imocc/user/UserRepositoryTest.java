package com.imocc.user;

import com.imocc.XunwuProjectApplicationTests;
import com.imocc.entity.User;
import com.imocc.repository.UserRepository;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;

/**
 * Created by Administrator on 2018/1/4.
 */
public class UserRepositoryTest extends XunwuProjectApplicationTests {
    private Logger logger= LoggerFactory.getLogger(this.getClass());
    @Autowired
    private UserRepository userRepository;

    @Test
    public void findOneUser(){
        User user = userRepository.findOne(2L);
        logger.info(user.toString());
    }
}
