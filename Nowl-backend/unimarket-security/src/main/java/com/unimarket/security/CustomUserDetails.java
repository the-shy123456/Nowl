package com.unimarket.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 自定义UserDetails实现类
 */
public class CustomUserDetails implements UserDetails {
    private Long userId;
    private String studentNo;
    private Integer accountStatus;
    private String password;
    private String schoolCode;
    private String campusCode;
    private Integer authStatus;
    private Set<String> roleCodes;
    private Set<String> permissionCodes;

    public CustomUserDetails(Long userId, String studentNo, String password, Integer accountStatus,
                             String schoolCode, String campusCode, Integer authStatus,
                             Set<String> roleCodes, Set<String> permissionCodes) {
        this.userId = userId;
        this.studentNo = studentNo;
        this.password = password;
        this.accountStatus = accountStatus;
        this.schoolCode = schoolCode;
        this.campusCode = campusCode;
        this.authStatus = authStatus;
        this.roleCodes = roleCodes == null ? new HashSet<>() : roleCodes;
        this.permissionCodes = permissionCodes == null ? new HashSet<>() : permissionCodes;
    }

    public Long getUserId() {
        return userId;
    }

    public String getSchoolCode() {
        return schoolCode;
    }

    public String getCampusCode() {
        return campusCode;
    }

    public Integer getAuthStatus() {
        return authStatus;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<String> roleAuthorities = roleCodes.stream()
                .map(code -> "ROLE_" + code)
                .collect(Collectors.toSet());
        roleAuthorities.add("ROLE_USER");

        Set<String> permissionAuthorities = new HashSet<>(permissionCodes);
        return java.util.stream.Stream.concat(roleAuthorities.stream(), permissionAuthorities.stream())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return studentNo;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountStatus == 0;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return accountStatus == 0;
    }
}
