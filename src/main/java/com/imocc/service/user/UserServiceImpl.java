package com.imocc.service.user;

import com.imocc.entity.Role;
import com.imocc.entity.User;
import com.imocc.repository.RoleRepository;
import com.imocc.repository.UserRepository;
import com.imocc.service.IUserService;
import com.imocc.service.ServiceResult;
import com.imocc.web.controller.house.UserDTO;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2018/1/6.
 */
@Service
public class UserServiceImpl implements IUserService {
    @Resource
    private UserRepository userRepository;
    @Resource
    private RoleRepository roleRepository;
    @Resource
    private ModelMapper modelMapper;

    @Override
    public User findUserByName(String userName){
        User user = userRepository.findUserByName(userName);
        if(user==null){
            return null;
        }
        List<Role> roleList = roleRepository.findRolesByUserId(user.getId());
        if(roleList==null|| roleList.isEmpty()){
            throw new DisabledException("权限非法");
        }
        List<GrantedAuthority> grantedAuthorityList=new ArrayList<GrantedAuthority>();
        roleList.forEach(role ->grantedAuthorityList.add(
                new SimpleGrantedAuthority("ROLE_"+role.getName())
        ) );
        user.setAuthoritieList(grantedAuthorityList);
        return user;
    }

    @Override
    public ServiceResult<UserDTO> findById(Long userId) {
        User user = userRepository.findOne(userId);
        if (user == null) {
            return ServiceResult.notFound();
        }
        UserDTO userDTO = modelMapper.map(user, UserDTO.class);
        return ServiceResult.of(userDTO);
    }
}
