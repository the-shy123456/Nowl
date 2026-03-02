package com.unimarket.security;

import com.unimarket.module.user.entity.UserInfo;
import com.unimarket.module.iam.service.IamAccessService;
import com.unimarket.module.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 自定义UserDetailsService实现类
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserService userService;
    @Autowired
    private IamAccessService iamAccessService;

    @Override
    public UserDetails loadUserByUsername(String phone) throws UsernameNotFoundException {
        // Token Subject 使用手机号
        UserInfo userInfo = userService.getByPhone(phone);
        if (userInfo == null) {
            throw new UsernameNotFoundException("用户不存在: " + phone);
        }

        java.util.Set<String> roleCodes = iamAccessService.getRoleCodes(userInfo.getUserId());
        java.util.Set<String> permissionCodes = iamAccessService.getPermissionCodes(userInfo.getUserId());

        // 构建认证主体（角色与权限来源于IAM，不再依赖is_admin字段）
        return new CustomUserDetails(
                userInfo.getUserId(),
                userInfo.getPhone(),
                userInfo.getPassword(),
                userInfo.getAccountStatus(),
                userInfo.getSchoolCode(),
                userInfo.getCampusCode(),
                userInfo.getAuthStatus(),
                roleCodes,
                permissionCodes
        );
    }
}
