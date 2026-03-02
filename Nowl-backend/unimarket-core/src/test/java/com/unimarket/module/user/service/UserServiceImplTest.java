package com.unimarket.module.user.service;

import com.unimarket.common.constant.CacheConstants;
import com.unimarket.common.exception.BusinessException;
import com.unimarket.common.result.ResultCode;
import com.unimarket.module.audit.mapper.AuditLoginTraceMapper;
import com.unimarket.module.iam.service.IamAccessService;
import com.unimarket.module.school.entity.SchoolInfo;
import com.unimarket.module.school.mapper.SchoolInfoMapper;
import com.unimarket.module.user.entity.UserInfo;
import com.unimarket.module.user.mapper.UserFollowMapper;
import com.unimarket.module.user.mapper.UserInfoMapper;
import com.unimarket.module.user.service.impl.UserServiceImpl;
import com.unimarket.module.user.vo.LoginVO;
import com.unimarket.module.user.vo.UserInfoVO;
import com.unimarket.security.util.JwtUtils;
import com.unimarket.common.utils.RedisCache;
import com.unimarket.common.config.SystemProperties;
import com.unimarket.module.risk.service.RiskControlService;
import com.unimarket.utils.SmsUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private UserInfoMapper userInfoMapper;
    @Mock
    private SchoolInfoMapper schoolInfoMapper;
    @Mock
    private UserFollowMapper userFollowMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private RedisCache redisCache;
    @Mock
    private IamAccessService iamAccessService;
    @Mock
    private RiskControlService riskControlService;
    @Mock
    private AuditLoginTraceMapper auditLoginTraceMapper;
    @Mock
    private SmsUtils smsUtils;
    @Mock
    private SystemProperties systemProperties;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("getPublicUserInfo: 不返回 IAM 权限字段")
    void getPublicUserInfo_withoutIamFields() {
        UserInfo user = buildUser(1001L);
        SchoolInfo school = new SchoolInfo();
        school.setSchoolName("测试大学");
        school.setCampusName("主校区");
        when(userInfoMapper.selectById(1001L)).thenReturn(user);
        when(schoolInfoMapper.selectOne(any())).thenReturn(school);

        UserInfoVO result = userService.getPublicUserInfo(1001L);

        assertEquals("测试大学", result.getSchoolName());
        assertEquals("主校区", result.getCampusName());
        assertNull(result.getRoleCodes());
        assertNull(result.getPermissionCodes());
        verify(iamAccessService, never()).getRoleCodes(any());
        verify(iamAccessService, never()).getPermissionCodes(any());
    }

    @Test
    @DisplayName("getUserInfo: 返回 IAM 角色和权限")
    void getUserInfo_withIamFields() {
        UserInfo user = buildUser(1002L);
        when(userInfoMapper.selectById(1002L)).thenReturn(user);
        when(iamAccessService.getRoleCodes(1002L)).thenReturn(Set.of("SUPER_ADMIN"));
        when(iamAccessService.getPermissionCodes(1002L)).thenReturn(Set.of("admin:dashboard:view"));

        UserInfoVO result = userService.getUserInfo(1002L);

        assertEquals(Set.of("SUPER_ADMIN"), result.getRoleCodes());
        assertEquals(Set.of("admin:dashboard:view"), result.getPermissionCodes());
    }

    @Test
    @DisplayName("refreshToken: refresh token 无效时抛出业务异常")
    void refreshToken_throwWhenRefreshTokenInvalid() {
        String refreshToken = "refresh-token";
        when(jwtUtils.validateToken(refreshToken)).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.refreshToken(refreshToken));
        assertEquals(ResultCode.REFRESH_TOKEN_INVALID.getCode(), exception.getCode());
    }

    @Test
    @DisplayName("refreshToken: 轮换 refresh token 并撤销旧 token")
    void refreshToken_rotateRefreshTokenAndRevokeOld() {
        String refreshToken = "old-refresh-token";
        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";

        when(jwtUtils.validateToken(refreshToken)).thenReturn(true);
        when(redisCache.hasKey(CacheConstants.REFRESH_TOKEN_ALLOWLIST + refreshToken)).thenReturn(true);
        when(jwtUtils.getUserIdFromToken(refreshToken)).thenReturn(1002L);

        UserInfo user = buildUser(1002L);
        user.setPhone("13800138000");
        when(userInfoMapper.selectById(1002L)).thenReturn(user);

        when(jwtUtils.generateToken(any())).thenReturn(newAccessToken);
        when(jwtUtils.generateRefreshToken(any())).thenReturn(newRefreshToken);
        when(jwtUtils.getRemainingSeconds(newRefreshToken)).thenReturn(3600L);

        LoginVO result = userService.refreshToken(refreshToken);

        assertEquals(newAccessToken, result.getToken());
        assertEquals(newRefreshToken, result.getRefreshToken());
        verify(redisCache).delete(CacheConstants.REFRESH_TOKEN_ALLOWLIST + refreshToken);
        verify(redisCache).set(eq(CacheConstants.REFRESH_TOKEN_ALLOWLIST + newRefreshToken), eq("1"), eq(3600L));
    }

    @Test
    @DisplayName("logout: access token 加入黑名单且撤销 refresh token")
    void logout_blacklistAccessTokenAndRevokeRefreshToken() {
        long userId = 1002L;
        String accessToken = "access-token";
        String refreshToken = "refresh-token";
        when(jwtUtils.getRemainingSeconds(accessToken)).thenReturn(120L);

        userService.logout(userId, accessToken, refreshToken);

        verify(redisCache).set(eq(CacheConstants.TOKEN_BLACKLIST + accessToken), eq("1"), eq(120L));
        verify(redisCache).delete(CacheConstants.REFRESH_TOKEN_ALLOWLIST + refreshToken);
    }

    private UserInfo buildUser(Long userId) {
        UserInfo user = new UserInfo();
        user.setUserId(userId);
        user.setNickName("tester");
        user.setSchoolCode("SC001");
        user.setCampusCode("CP001");
        return user;
    }
}
