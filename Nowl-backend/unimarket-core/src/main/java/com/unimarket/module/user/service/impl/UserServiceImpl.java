package com.unimarket.module.user.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.unimarket.common.constant.CacheConstants;
import com.unimarket.common.config.SystemProperties;
import com.unimarket.common.constant.SystemConstants;
import com.unimarket.common.enums.AuthStatus;
import com.unimarket.common.enums.RunnableStatus;
import com.unimarket.common.exception.BusinessException;
import com.unimarket.common.result.ResultCode;
import com.unimarket.common.utils.RedisCache;
import com.unimarket.common.security.HttpRequestIpResolver;
import com.unimarket.module.audit.entity.AuditLoginTrace;
import com.unimarket.module.audit.mapper.AuditLoginTraceMapper;
import com.unimarket.module.iam.service.IamAccessService;
import com.unimarket.module.risk.dto.RiskContext;
import com.unimarket.module.risk.enums.RiskAction;
import com.unimarket.module.risk.enums.RiskEventType;
import com.unimarket.module.risk.service.RiskControlService;
import com.unimarket.module.risk.vo.RiskDecisionResult;
import com.unimarket.module.school.entity.SchoolInfo;
import com.unimarket.module.school.mapper.SchoolInfoMapper;
import com.unimarket.module.user.dto.ResetPasswordDTO;
import com.unimarket.module.user.dto.UserLoginDTO;
import com.unimarket.module.user.dto.UserRegisterDTO;
import com.unimarket.module.user.dto.UserUpdateDTO;
import com.unimarket.module.user.entity.UserFollow;
import com.unimarket.module.user.entity.UserInfo;
import com.unimarket.module.user.mapper.UserFollowMapper;
import com.unimarket.module.user.mapper.UserInfoMapper;
import com.unimarket.module.user.service.UserService;
import com.unimarket.module.user.vo.FollowUserVO;
import com.unimarket.module.user.vo.LoginVO;
import com.unimarket.module.user.vo.UserInfoVO;
import com.unimarket.security.util.JwtUtils;
import com.unimarket.utils.SmsUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserService {
    private final StringRedisTemplate redisTemplate;
    private final UserInfoMapper userInfoMapper;
    private final SchoolInfoMapper schoolInfoMapper;
    private final UserFollowMapper userFollowMapper;
    @Lazy
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RedisCache redisCache;
    private final IamAccessService iamAccessService;
    private final RiskControlService riskControlService;
    private final AuditLoginTraceMapper auditLoginTraceMapper;
    private final SmsUtils smsUtils;
    private final SystemProperties systemProperties;
    private final HttpRequestIpResolver httpRequestIpResolver;

    @Override
    public void register(UserRegisterDTO dto) {
        // 0. 校验验证码
        String cacheCode = redisCache.get("sms:code:" + dto.getPhone(), String.class);
        if (cacheCode == null || !cacheCode.equals(dto.getCode())) {
            throw new BusinessException("验证码错误或已失效");
        }
        // 验证通过后删除验证码，防止重复使用
        redisCache.delete("sms:code:" + dto.getPhone());

        // 1. 检查手机号是否已存在
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getPhone, dto.getPhone());
        UserInfo existUser = userInfoMapper.selectOne(wrapper);
        if (existUser != null) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }

        // 2. 创建用户
        UserInfo userInfo = new UserInfo();
        userInfo.setPhone(dto.getPhone());
        userInfo.setPassword(passwordEncoder.encode(dto.getPassword()));
        userInfo.setNickName(dto.getNickName());
        userInfo.setAuthStatus(0);
        userInfo.setCreditScore(SystemConstants.INITIAL_CREDIT_SCORE);
        userInfo.setAccountStatus(0);
        userInfo.setRunnableStatus(0);
        userInfo.setMoney(BigDecimal.valueOf(SystemConstants.INITIAL_BALANCE));
        userInfo.setFollowCount(0);
        userInfo.setFanCount(0);
        userInfo.setImageUrl(systemProperties.getDefaultAvatar());

        // 新增字段默认值
        userInfo.setGender(0); // 未知
        userInfo.setUserType(0); // 游客
        userInfo.setGrade(null); // 待认证补全

        // 学号设为空，待认证时补全
        userInfo.setStudentNo(null);
        // 未认证用户学校/校区编码为空，前端根据空值显示"游客"
        userInfo.setSchoolCode(null);
        userInfo.setCampusCode(null);

        int result = userInfoMapper.insert(userInfo);
        if (result <= 0) {
            throw new BusinessException("注册失败");
        }

        log.info("用户注册成功，手机号：{}", dto.getPhone());
    }


    @Override
    public LoginVO login(UserLoginDTO dto) {
        String requestIp = resolveRequestIp();
        String requestDevice = resolveRequestDevice();

        // 0. 校验图形验证码
        String captchaKey = "captcha:" + dto.getUuid();
        String captchaCode = redisTemplate.opsForValue().get(captchaKey);
        if (captchaCode == null) {
            saveLoginTrace(null, dto.getPhone(), requestIp, requestDevice, "FAIL", "CAPTCHA_EXPIRED", "low");
            throw new BusinessException("图形验证码已失效，请刷新");
        }
        if (!captchaCode.equalsIgnoreCase(dto.getCode())) {
            saveLoginTrace(null, dto.getPhone(), requestIp, requestDevice, "FAIL", "CAPTCHA_INVALID", "low");
            throw new BusinessException("图形验证码错误");
        }
        // 验证通过后删除验证码
        redisTemplate.delete(captchaKey);

        // 1. 查询用户
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getPhone, dto.getPhone());
        UserInfo userInfo = userInfoMapper.selectOne(wrapper);

        Map<String, Object> loginFeatures = new HashMap<>();
        loginFeatures.put("phone", dto.getPhone());
        loginFeatures.put("ip", requestIp);
        loginFeatures.put("device", requestDevice);

        Map<String, Object> loginPayload = new HashMap<>();
        loginPayload.put("phone", dto.getPhone());
        loginPayload.put("ip", requestIp);
        loginPayload.put("device", requestDevice);

        RiskDecisionResult decisionResult = riskControlService.evaluate(RiskContext.builder()
                .eventType(RiskEventType.LOGIN)
                .userId(userInfo == null ? null : userInfo.getUserId())
                .subjectId(userInfo == null ? dto.getPhone() : String.valueOf(userInfo.getUserId()))
                .schoolCode(userInfo == null ? null : userInfo.getSchoolCode())
                .campusCode(userInfo == null ? null : userInfo.getCampusCode())
                .requestIp(requestIp)
                .deviceId(requestDevice)
                .features(loginFeatures)
                .rawPayload(loginPayload)
                .build());

        if (decisionResult.getAction() != RiskAction.ALLOW) {
            saveLoginTrace(
                    userInfo == null ? null : userInfo.getUserId(),
                    dto.getPhone(),
                    requestIp,
                    requestDevice,
                    "CHALLENGE",
                    decisionResult.getReason(),
                    decisionResult.getRiskLevel()
            );
            throw new BusinessException("当前登录行为触发风控策略，请稍后再试");
        }

        if (userInfo == null) {
            saveLoginTrace(null, dto.getPhone(), requestIp, requestDevice, "FAIL", "USER_NOT_FOUND", decisionResult.getRiskLevel());
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 2. 验证密码
        if (!passwordEncoder.matches(dto.getPassword(), userInfo.getPassword())) {
            saveLoginTrace(userInfo.getUserId(), dto.getPhone(), requestIp, requestDevice, "FAIL", "PASSWORD_ERROR", decisionResult.getRiskLevel());
            throw new BusinessException(ResultCode.USER_PASSWORD_ERROR);
        }

        // 3. 检查账号状态
        if (userInfo.getAccountStatus() == 1) {
            saveLoginTrace(userInfo.getUserId(), dto.getPhone(), requestIp, requestDevice, "FAIL", "ACCOUNT_DISABLED", decisionResult.getRiskLevel());
            throw new BusinessException(ResultCode.USER_ACCOUNT_DISABLED);
        }

        // 4. 生成JWT Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userInfo.getPhone());
        claims.put("userId", userInfo.getUserId());
        String token = jwtUtils.generateToken(claims);
        String refreshToken = jwtUtils.generateRefreshToken(claims);
        registerRefreshToken(refreshToken);

        // 5. 获取用户详细信息
        UserInfoVO userInfoVO = getUserInfo(userInfo.getUserId());

        saveLoginTrace(userInfo.getUserId(), dto.getPhone(), requestIp, requestDevice, "SUCCESS", null, decisionResult.getRiskLevel());

        log.info("用户登录成功，手机号：{}", dto.getPhone());

        return new LoginVO(token, userInfoVO, refreshToken);
    }

    @Override
    public LoginVO refreshToken(String refreshToken) {
        // 1. 验证RefreshToken
        if (!jwtUtils.validateToken(refreshToken) || !isRefreshTokenAllowed(refreshToken)) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
        }

        // 2. 获取用户信息
        Long userId = jwtUtils.getUserIdFromToken(refreshToken);
        if (userId == null) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
        }

        UserInfo userInfo = userInfoMapper.selectById(userId);
        if (userInfo == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 3. 生成新Access Token + 轮换Refresh Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userInfo.getPhone());
        claims.put("userId", userInfo.getUserId());
        String newAccessToken = jwtUtils.generateToken(claims);
        String newRefreshToken = jwtUtils.generateRefreshToken(claims);

        // 旧refresh token失效，新token入白名单
        revokeRefreshToken(refreshToken);
        registerRefreshToken(newRefreshToken);

        // 4. 获取用户详情 (可选，或者只返回token)
        UserInfoVO userInfoVO = getUserInfo(userId);

        return new LoginVO(newAccessToken, userInfoVO, newRefreshToken);
    }

    @Override
    public UserInfoVO getCurrentUserInfo(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.USER_NOT_LOGIN);
        }
        return getUserInfo(userId);
    }

    @Override
    public UserInfoVO getUserInfo(Long userId) {
        return buildUserInfo(userId, true);
    }

    @Override
    public UserInfoVO getPublicUserInfo(Long userId) {
        return buildUserInfo(userId, false);
    }

    private UserInfoVO buildUserInfo(Long userId, boolean includeIamAuth) {
        // 1. 查询用户信息
        UserInfo userInfo = userInfoMapper.selectById(userId);
        if (userInfo == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 2. 查询学校信息
        SchoolInfo schoolInfo = null;
        try {
            if (userInfo.getSchoolCode() != null && userInfo.getCampusCode() != null) {
                LambdaQueryWrapper<SchoolInfo> schoolWrapper = new LambdaQueryWrapper<>();
                schoolWrapper.eq(SchoolInfo::getSchoolCode, userInfo.getSchoolCode())
                        .eq(SchoolInfo::getCampusCode, userInfo.getCampusCode());
                schoolInfo = schoolInfoMapper.selectOne(schoolWrapper);
            }
        } catch (Exception e) {
            log.warn("查询学校信息失败: userId={}, err={}", userId, e.getMessage());
            // 忽略错误，不影响登录
        }

        // 3. 转换为VO
        UserInfoVO vo = BeanUtil.copyProperties(userInfo, UserInfoVO.class);
        if (schoolInfo != null) {
            vo.setSchoolName(schoolInfo.getSchoolName());
            vo.setCampusName(schoolInfo.getCampusName());
        }
        if (includeIamAuth) {
            vo.setRoleCodes(iamAccessService.getRoleCodes(userId));
            vo.setPermissionCodes(iamAccessService.getPermissionCodes(userId));
        }

        return vo;
    }

    @Override
    public void updateUserInfo(Long userId, UserUpdateDTO dto) {
        // 0. 检查用户是否登录
        if (userId == null) {
            throw new BusinessException(ResultCode.USER_NOT_LOGIN);
        }

        // 1. 查询用户信息
        UserInfo userInfo = userInfoMapper.selectById(userId);
        if (userInfo == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 2. 更新用户信息
        if (dto.getNickName() != null) {
            userInfo.setNickName(dto.getNickName());
        }
        if (dto.getAvatar() != null) {
            userInfo.setImageUrl(dto.getAvatar());
        }
        if (dto.getCertImage() != null) userInfo.setCertImage(dto.getCertImage());
        if (dto.getSelfImage() != null) userInfo.setSelfImage(dto.getSelfImage());

        if (dto.getPhone() != null) {
            userInfo.setPhone(dto.getPhone());
        }

        // 认证信息更新
        if (dto.getUserName() != null) userInfo.setUserName(dto.getUserName());
        if (dto.getStudentNo() != null) userInfo.setStudentNo(dto.getStudentNo());
        if (dto.getSchoolCode() != null) userInfo.setSchoolCode(dto.getSchoolCode());
        if (dto.getCampusCode() != null) userInfo.setCampusCode(dto.getCampusCode());
        if (dto.getGender() != null) userInfo.setGender(dto.getGender());
        if (dto.getGrade() != null) userInfo.setGrade(dto.getGrade());

        // 如果用户主动提交审核
        if (dto.getAuthStatus() != null && dto.getAuthStatus() == 1) {
            userInfo.setAuthStatus(1); // 变更为待审核
        }

        // 3. 保存到数据库
        userInfoMapper.updateById(userInfo);

        log.info("用户信息更新成功: userId={}", userId);
    }

    @Override
    public void applyRunner(Long userId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.USER_NOT_LOGIN);
        }
        UserInfo userInfo = userInfoMapper.selectById(userId);
        if (userInfo == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        if (userInfo.getAuthStatus() == null || !userInfo.getAuthStatus().equals(AuthStatus.APPROVED.getCode())) {
            throw new BusinessException("请先完成校园认证");
        }
        Integer status = userInfo.getRunnableStatus();
        if (RunnableStatus.APPROVED.getCode().equals(status)) {
            throw new BusinessException("您已是跑腿员");
        }
        if (RunnableStatus.PENDING.getCode().equals(status)) {
            throw new BusinessException("已提交申请，请耐心等待审核");
        }
        userInfo.setRunnableStatus(RunnableStatus.PENDING.getCode());
        userInfoMapper.updateById(userInfo);
        log.info("跑腿员申请已提交: userId={}", userId);
    }

    @Override
    public UserInfo getByStudentNo(String studentNo) {
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getStudentNo, studentNo);
        return userInfoMapper.selectOne(wrapper);
    }

    @Override
    public UserInfo getByPhone(String phone) {
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getPhone, phone);
        return userInfoMapper.selectOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void followUser(Long userId, Long targetUserId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.USER_NOT_LOGIN);
        }

        if (userId.equals(targetUserId)) {
            throw new BusinessException("不能关注自己");
        }
        
        // 1. 检查目标用户是否存在
        UserInfo targetUser = userInfoMapper.selectById(targetUserId);
        if (targetUser == null) {
            throw new BusinessException("关注的用户不存在");
        }

        UserInfo user = userInfoMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        riskControlService.assertAllowed(RiskContext.builder()
                .eventType(RiskEventType.FOLLOW_USER)
                .userId(userId)
                .subjectId(String.valueOf(userId))
                .schoolCode(user.getSchoolCode())
                .campusCode(user.getCampusCode())
                .requestIp(resolveRequestIp())
                .deviceId(resolveRequestDevice())
                .features(Map.of(
                        "targetUserId", targetUserId
                ))
                .rawPayload(Map.of(
                        "targetUserId", targetUserId,
                        "sourceUserId", userId
                ))
                .build());

        // 2. 检查是否已关注
        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollow::getUserId, userId)
                .eq(UserFollow::getFollowedUserId, targetUserId);
        UserFollow follow = userFollowMapper.selectOne(wrapper);

        if (follow != null) {
            if (follow.getIsCancel() == 0) {
                return; // 已经关注，直接返回
            }
            // 之前关注过但取消了，重新关注
            follow.setIsCancel(0);
            follow.setFollowTime(java.time.LocalDateTime.now());
            userFollowMapper.updateById(follow);
        } else {
            // 新增关注记录
            follow = new UserFollow();
            follow.setUserId(userId);
            follow.setFollowedUserId(targetUserId);
            follow.setFollowTime(java.time.LocalDateTime.now());
            follow.setIsCancel(0);
            userFollowMapper.insert(follow);
        }

        // 3. 更新粉丝数和关注数
        // 关注者：关注数+1
        user.setFollowCount(user.getFollowCount() + 1);
        userInfoMapper.updateById(user);
        
        // 被关注者：粉丝数+1
        targetUser.setFanCount(targetUser.getFanCount() + 1);
        userInfoMapper.updateById(targetUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfollowUser(Long userId, Long targetUserId) {
        if (userId == null) {
            throw new BusinessException(ResultCode.USER_NOT_LOGIN);
        }

        // 1. 查询关注记录
        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollow::getUserId, userId)
                .eq(UserFollow::getFollowedUserId, targetUserId)
                .eq(UserFollow::getIsCancel, 0);
        UserFollow follow = userFollowMapper.selectOne(wrapper);

        if (follow == null) {
            return; // 未关注，直接返回
        }

        // 2. 更新为已取消
        follow.setIsCancel(1);
        userFollowMapper.updateById(follow);

        // 3. 更新粉丝数和关注数
        // 关注者：关注数-1
        UserInfo user = userInfoMapper.selectById(userId);
        if (user.getFollowCount() > 0) {
            user.setFollowCount(user.getFollowCount() - 1);
            userInfoMapper.updateById(user);
        }

        // 被关注者：粉丝数-1
        UserInfo targetUser = userInfoMapper.selectById(targetUserId);
        if (targetUser.getFanCount() > 0) {
            targetUser.setFanCount(targetUser.getFanCount() - 1);
            userInfoMapper.updateById(targetUser);
        }
    }

    @Override
    public void sendSmsCode(String phone) {
        String key = "sms:code:" + phone;
        String limitKey = "sms:limit:" + phone;
        if (redisCache.get(limitKey, String.class) != null) {
             throw new BusinessException("发送过于频繁，请稍后再试");
        }
        
        // 生成6位随机验证码
        String code = cn.hutool.core.util.RandomUtil.randomNumbers(6);
        
        // 发送短信
        boolean success = smsUtils.sendSmsCode(phone, code);
        if (!success) {
            throw new BusinessException("短信发送失败，请稍后重试");
        }
        
        // 存入Redis，验证码5分钟有效
        redisCache.set(key, code, 300);
        // 设置限流Key，60秒有效
        redisCache.set(limitKey, "1", 60);
        
        log.info("短信验证码已发送，phone={}", maskPhone(phone));
    }

    @Override
    public void resetPassword(ResetPasswordDTO dto) {
        // 1. 校验验证码
        String cacheCode = redisCache.get("sms:code:" + dto.getPhone(), String.class);
        if (cacheCode == null || !cacheCode.equals(dto.getCode())) {
            throw new BusinessException("验证码错误或已失效");
        }
        redisCache.delete("sms:code:" + dto.getPhone());

        // 2. 查询用户
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getPhone, dto.getPhone());
        UserInfo userInfo = userInfoMapper.selectOne(wrapper);
        if (userInfo == null) {
            throw new BusinessException("该手机号未注册");
        }

        // 3. 更新密码
        userInfo.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userInfoMapper.updateById(userInfo);

        log.info("用户重置密码成功，phone={}", maskPhone(dto.getPhone()));
    }

    @Override
    public String getCaptcha(String uuid) {
        // 生成图形验证码
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(200, 100, 4, 20);
        String code = lineCaptcha.getCode();
        
        // 存入Redis，5分钟有效
        redisTemplate.opsForValue().set("captcha:" + uuid, code, 5, TimeUnit.MINUTES);
        
        // 返回Base64图片
        return lineCaptcha.getImageBase64();
    }

    @Override
    public void logout(Long userId, String accessToken, String refreshToken) {
        if (StrUtil.isNotBlank(accessToken)) {
            // 计算Token剩余有效时间
            long remainingSeconds = jwtUtils.getRemainingSeconds(accessToken);
            if (remainingSeconds > 0) {
                // 加入Redis黑名单
                redisCache.set(CacheConstants.TOKEN_BLACKLIST + accessToken, "1", remainingSeconds);
                log.info("Token已加入黑名单: userId={}, remainingSeconds={}", userId, remainingSeconds);
            }
        }

        revokeRefreshToken(refreshToken);
        log.info("用户退出登录: userId={}", userId);
    }

    @Override
    public boolean isFollowing(Long userId, Long targetUserId) {
        if (userId == null || targetUserId == null || userId.equals(targetUserId)) {
            return false;
        }
        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollow::getUserId, userId)
                .eq(UserFollow::getFollowedUserId, targetUserId)
                .eq(UserFollow::getIsCancel, 0);
        return userFollowMapper.selectCount(wrapper) > 0;
    }

    @Override
    public Page<FollowUserVO> getFollowingList(Long userId, Long currentUserId, int pageNum, int pageSize) {
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        // 1. 查询关注列表
        Page<UserFollow> page = new Page<>(safePageNum, safePageSize);
        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollow::getUserId, userId)
                .eq(UserFollow::getIsCancel, 0)
                .orderByDesc(UserFollow::getFollowTime);

        Page<UserFollow> followPage = userFollowMapper.selectPage(page, wrapper);

        // 2. 转换为VO
        Page<FollowUserVO> resultPage = new Page<>(safePageNum, safePageSize);
        resultPage.setTotal(followPage.getTotal());

        List<UserFollow> follows = followPage.getRecords();
        if (follows.isEmpty()) {
            resultPage.setRecords(new ArrayList<>());
            return resultPage;
        }

        // 1. 批量查询用户信息 (解决 N+1)
        List<Long> followedUserIds = follows.stream()
                .map(UserFollow::getFollowedUserId)
                .collect(Collectors.toList());
        List<UserInfo> targetUsers = userInfoMapper.selectBatchIds(followedUserIds);
        Map<Long, UserInfo> userMap = targetUsers.stream()
                .collect(Collectors.toMap(UserInfo::getUserId, u -> u));

        // 2. 批量查询学校信息 (解决 N+1)
        Set<String> schoolCodes = targetUsers.stream()
                .map(UserInfo::getSchoolCode)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
        Map<String, SchoolInfo> schoolMap = new HashMap<>();
        if (!schoolCodes.isEmpty()) {
            LambdaQueryWrapper<SchoolInfo> schoolWrapper = new LambdaQueryWrapper<>();
            schoolWrapper.in(SchoolInfo::getSchoolCode, schoolCodes);
            List<SchoolInfo> schools = schoolInfoMapper.selectList(schoolWrapper);
            schoolMap = schools.stream().collect(Collectors.toMap(s -> s.getSchoolCode() + "|" + s.getCampusCode(), s -> s));
        }

        // 3. 构建结果集
        List<FollowUserVO> voList = new ArrayList<>();
        for (UserFollow follow : follows) {
            UserInfo targetUser = userMap.get(follow.getFollowedUserId());
            if (targetUser != null) {
                FollowUserVO vo = new FollowUserVO();
                vo.setUserId(targetUser.getUserId());
                vo.setNickName(targetUser.getNickName());
                vo.setImageUrl(targetUser.getImageUrl());
                vo.setAuthStatus(targetUser.getAuthStatus());
                vo.setCreditScore(targetUser.getCreditScore());
                vo.setFollowTime(follow.getFollowTime());
                
                // 填充学校名称
                SchoolInfo sInfo = schoolMap.get(targetUser.getSchoolCode() + "|" + targetUser.getCampusCode());
                if (sInfo != null) {
                    vo.setSchoolName(sInfo.getSchoolName());
                    vo.setCampusName(sInfo.getCampusName());
                }
                
                // 辅助逻辑：关注判定
                vo.setIsMutual(isFollowing(follow.getFollowedUserId(), userId));
                vo.setIsFollowed(currentUserId != null && isFollowing(currentUserId, follow.getFollowedUserId()));
                voList.add(vo);
            }
        }
        resultPage.setRecords(voList);
        return resultPage;
    }

    @Override
    public Page<FollowUserVO> getFollowerList(Long userId, Long currentUserId, int pageNum, int pageSize) {
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        // 1. 查询粉丝列表
        Page<UserFollow> page = new Page<>(safePageNum, safePageSize);
        LambdaQueryWrapper<UserFollow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserFollow::getFollowedUserId, userId)
                .eq(UserFollow::getIsCancel, 0)
                .orderByDesc(UserFollow::getFollowTime);

        Page<UserFollow> followPage = userFollowMapper.selectPage(page, wrapper);

        // 2. 转换为VO
        Page<FollowUserVO> resultPage = new Page<>(safePageNum, safePageSize);
        resultPage.setTotal(followPage.getTotal());

        List<UserFollow> followers = followPage.getRecords();
        if (followers.isEmpty()) {
            resultPage.setRecords(new ArrayList<>());
            return resultPage;
        }

        // 1. 批量查询用户信息 (解决 N+1)
        List<Long> fanUserIds = followers.stream()
                .map(UserFollow::getUserId)
                .collect(Collectors.toList());
        List<UserInfo> fanUsers = userInfoMapper.selectBatchIds(fanUserIds);
        Map<Long, UserInfo> userMap = fanUsers.stream()
                .collect(Collectors.toMap(UserInfo::getUserId, u -> u));

        // 2. 批量查询学校信息 (解决 N+1)
        Set<String> schoolCodes = fanUsers.stream()
                .map(UserInfo::getSchoolCode)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
        Map<String, SchoolInfo> schoolMap = new HashMap<>();
        if (!schoolCodes.isEmpty()) {
            LambdaQueryWrapper<SchoolInfo> schoolWrapper = new LambdaQueryWrapper<>();
            schoolWrapper.in(SchoolInfo::getSchoolCode, schoolCodes);
            List<SchoolInfo> schools = schoolInfoMapper.selectList(schoolWrapper);
            schoolMap = schools.stream().collect(Collectors.toMap(s -> s.getSchoolCode() + "|" + s.getCampusCode(), s -> s));
        }

        // 3. 构建结果集
        List<FollowUserVO> voList = new ArrayList<>();
        for (UserFollow follow : followers) {
            UserInfo fanUser = userMap.get(follow.getUserId());
            if (fanUser != null) {
                FollowUserVO vo = new FollowUserVO();
                vo.setUserId(fanUser.getUserId());
                vo.setNickName(fanUser.getNickName());
                vo.setImageUrl(fanUser.getImageUrl());
                vo.setAuthStatus(fanUser.getAuthStatus());
                vo.setCreditScore(fanUser.getCreditScore());
                vo.setFollowTime(follow.getFollowTime());
                
                SchoolInfo sInfo = schoolMap.get(fanUser.getSchoolCode() + "|" + fanUser.getCampusCode());
                if (sInfo != null) {
                    vo.setSchoolName(sInfo.getSchoolName());
                    vo.setCampusName(sInfo.getCampusName());
                }
                
                vo.setIsMutual(isFollowing(userId, follow.getUserId()));
                vo.setIsFollowed(currentUserId != null && isFollowing(currentUserId, follow.getUserId()));
                voList.add(vo);
            }
        }
        resultPage.setRecords(voList);
        return resultPage;
    }

    private void saveLoginTrace(Long userId,
                                String phone,
                                String ip,
                                String device,
                                String result,
                                String failReason,
                                String riskLevel) {
        AuditLoginTrace trace = new AuditLoginTrace();
        trace.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        trace.setUserId(userId);
        trace.setPhone(phone);
        trace.setIp(ip);
        trace.setDeviceId(device);
        trace.setGeo(null);
        trace.setLoginResult(result);
        trace.setFailReason(failReason);
        trace.setRiskLevel((riskLevel == null || riskLevel.isBlank()) ? "low" : riskLevel);
        trace.setCreateTime(LocalDateTime.now());
        auditLoginTraceMapper.insert(trace);
    }

    private String resolveRequestIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        jakarta.servlet.http.HttpServletRequest request = attributes.getRequest();
        if (request == null) {
            return null;
        }
        return httpRequestIpResolver.resolve(request);
    }

    private String resolveRequestDevice() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null || attributes.getRequest() == null) {
            return null;
        }
        return attributes.getRequest().getHeader("User-Agent");
    }

    private void registerRefreshToken(String refreshToken) {
        if (StrUtil.isBlank(refreshToken)) {
            return;
        }
        long remainingSeconds = jwtUtils.getRemainingSeconds(refreshToken);
        if (remainingSeconds > 0) {
            redisCache.set(CacheConstants.REFRESH_TOKEN_ALLOWLIST + refreshToken, "1", remainingSeconds);
        }
    }

    private boolean isRefreshTokenAllowed(String refreshToken) {
        if (StrUtil.isBlank(refreshToken)) {
            return false;
        }
        return Boolean.TRUE.equals(redisCache.hasKey(CacheConstants.REFRESH_TOKEN_ALLOWLIST + refreshToken));
    }

    private void revokeRefreshToken(String refreshToken) {
        if (StrUtil.isBlank(refreshToken)) {
            return;
        }
        redisCache.delete(CacheConstants.REFRESH_TOKEN_ALLOWLIST + refreshToken);
    }

    private String maskPhone(String phone) {
        if (StrUtil.isBlank(phone) || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
