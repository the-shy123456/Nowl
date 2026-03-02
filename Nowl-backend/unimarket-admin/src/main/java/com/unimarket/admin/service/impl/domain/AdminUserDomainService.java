package com.unimarket.admin.service.impl.domain;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimarket.admin.service.impl.support.AdminActionLockSupport;
import com.unimarket.admin.service.impl.support.AdminScopeSupport;
import com.unimarket.admin.service.impl.support.AdminSchoolInfoSupport;
import com.unimarket.common.enums.AuthStatus;
import com.unimarket.common.enums.NoticeType;
import com.unimarket.common.enums.RunnableStatus;
import com.unimarket.common.exception.BusinessException;
import com.unimarket.common.result.PageQuery;
import com.unimarket.common.result.ResultCode;
import com.unimarket.module.iam.entity.IamAdminScopeBinding;
import com.unimarket.module.iam.service.IamAccessService;
import com.unimarket.module.notice.service.NoticeService;
import com.unimarket.module.school.entity.SchoolInfo;
import com.unimarket.module.user.entity.UserInfo;
import com.unimarket.module.user.mapper.UserInfoMapper;
import com.unimarket.module.user.service.CreditScoreService;
import com.unimarket.module.user.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserDomainService {

    private final UserInfoMapper userInfoMapper;
    private final CreditScoreService creditScoreService;
    private final NoticeService noticeService;
    private final IamAccessService iamAccessService;
    private final AdminActionLockSupport actionLockSupport;
    private final AdminScopeSupport scopeSupport;
    private final AdminSchoolInfoSupport schoolInfoSupport;

    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long operatorId, Long userId, Integer status) {
        UserInfo user = userInfoMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        iamAccessService.assertCanManageScope(operatorId, user.getSchoolCode(), user.getCampusCode());
        user.setAccountStatus(status); // 0-正常, 1-禁用
        userInfoMapper.updateById(user);
        log.info("用户状态更新: userId={}, status={}", userId, status);
    }

    public Page<UserInfoVO> getUserList(Long operatorId,
                                        PageQuery query,
                                        String keyword,
                                        String schoolCode,
                                        String campusCode,
                                        Integer accountStatus,
                                        Integer authStatus) {
        List<IamAdminScopeBinding> scopes = scopeSupport.getOperatorScopes(operatorId);

        Page<UserInfo> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(UserInfo::getNickName, keyword)
                    .or().like(UserInfo::getPhone, keyword));
        }
        wrapper.eq(StrUtil.isNotBlank(schoolCode), UserInfo::getSchoolCode, schoolCode)
                .eq(StrUtil.isNotBlank(campusCode), UserInfo::getCampusCode, campusCode)
                .eq(accountStatus != null, UserInfo::getAccountStatus, accountStatus)
                .eq(authStatus != null, UserInfo::getAuthStatus, authStatus);
        wrapper.orderByDesc(UserInfo::getCreateTime);
        scopeSupport.applyScopeFilter(wrapper, scopes, UserInfo::getSchoolCode, UserInfo::getCampusCode);
        Page<UserInfo> userPage = userInfoMapper.selectPage(page, wrapper);
        List<UserInfo> records = userPage.getRecords();
        if (records.isEmpty()) {
            return new Page<UserInfoVO>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        }

        Map<String, SchoolInfo> schoolMap = schoolInfoSupport.buildSchoolInfoMap(records, UserInfo::getSchoolCode);
        List<UserInfoVO> vos = records.stream().map(user -> {
            UserInfoVO vo = BeanUtil.copyProperties(user, UserInfoVO.class);
            schoolInfoSupport.fillSchoolCampusNames(
                    user.getSchoolCode(),
                    user.getCampusCode(),
                    schoolMap,
                    vo::setSchoolName,
                    vo::setCampusName
            );
            return vo;
        }).toList();

        return new Page<UserInfoVO>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal()).setRecords(vos);
    }

    public Page<UserInfoVO> getPendingAuthUsers(Long operatorId, PageQuery query, String schoolCode, String campusCode) {
        List<IamAdminScopeBinding> scopes = scopeSupport.getOperatorScopes(operatorId);

        Page<UserInfo> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getAuthStatus, AuthStatus.PENDING.getCode())
                .eq(StrUtil.isNotBlank(schoolCode), UserInfo::getSchoolCode, schoolCode)
                .eq(StrUtil.isNotBlank(campusCode), UserInfo::getCampusCode, campusCode)
                .orderByAsc(UserInfo::getUpdateTime);
        scopeSupport.applyScopeFilter(wrapper, scopes, UserInfo::getSchoolCode, UserInfo::getCampusCode);

        Page<UserInfo> userPage = userInfoMapper.selectPage(page, wrapper);
        List<UserInfo> records = userPage.getRecords();

        if (records.isEmpty()) {
            return new Page<UserInfoVO>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        }

        Map<String, SchoolInfo> schoolMap = schoolInfoSupport.buildSchoolInfoMap(records, UserInfo::getSchoolCode);
        List<UserInfoVO> vos = records.stream()
                .map(user -> {
                    UserInfoVO vo = BeanUtil.copyProperties(user, UserInfoVO.class);
                    schoolInfoSupport.fillSchoolCampusNames(
                            user.getSchoolCode(),
                            user.getCampusCode(),
                            schoolMap,
                            vo::setSchoolName,
                            vo::setCampusName
                    );
                    return vo;
                })
                .toList();

        return new Page<UserInfoVO>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal()).setRecords(vos);
    }

    @Transactional(rollbackFor = Exception.class)
    public void auditUserAuth(Long operatorId, Long userId, Integer status, String reason) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("审核状态仅支持 1-通过 或 0-驳回");
        }
        String lockKey = "admin:audit:user-auth:" + userId;
        actionLockSupport.withLock(lockKey, () -> doAuditUserAuth(operatorId, userId, status, reason));
    }

    private void doAuditUserAuth(Long operatorId, Long userId, Integer status, String reason) {
        UserInfo user = userInfoMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        iamAccessService.assertCanManageScope(operatorId, user.getSchoolCode(), user.getCampusCode());
        boolean approved = status == 1;
        Integer targetStatus = approved ? AuthStatus.APPROVED.getCode() : AuthStatus.REJECTED.getCode();
        Integer currentStatus = user.getAuthStatus();
        if (targetStatus.equals(currentStatus)) {
            log.info("实名认证审核重复请求已忽略: operatorId={}, userId={}, authStatus={}", operatorId, userId, targetStatus);
            return;
        }
        if (!AuthStatus.PENDING.getCode().equals(currentStatus)) {
            throw new BusinessException("该实名认证申请已被处理，请刷新后重试");
        }
        user.setAuthStatus(targetStatus);
        userInfoMapper.updateById(user);

        String title = approved ? "实名认证审核通过" : "实名认证审核未通过";
        String content;
        if (approved) {
            content = "您的实名认证已通过审核，现在可以正常发布与交易。";
        } else {
            String rejectReason = StrUtil.isBlank(reason) ? "资料不符合要求" : reason;
            content = "您的实名认证未通过审核。原因：" + rejectReason + "。";
        }
        noticeService.sendNotice(userId, title, content, NoticeType.SYSTEM.getCode());
        log.info("实名认证审核完成: operatorId={}, userId={}, approved={}", operatorId, userId, approved);
    }

    public Page<UserInfoVO> getPendingRunnerUsers(Long operatorId, PageQuery query, String schoolCode, String campusCode) {
        List<IamAdminScopeBinding> scopes = scopeSupport.getOperatorScopes(operatorId);

        Page<UserInfo> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getRunnableStatus, RunnableStatus.PENDING.getCode())
                .eq(StrUtil.isNotBlank(schoolCode), UserInfo::getSchoolCode, schoolCode)
                .eq(StrUtil.isNotBlank(campusCode), UserInfo::getCampusCode, campusCode)
                .orderByAsc(UserInfo::getUpdateTime);
        scopeSupport.applyScopeFilter(wrapper, scopes, UserInfo::getSchoolCode, UserInfo::getCampusCode);

        Page<UserInfo> userPage = userInfoMapper.selectPage(page, wrapper);
        List<UserInfo> records = userPage.getRecords();
        if (records.isEmpty()) {
            return new Page<UserInfoVO>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        }

        Map<String, SchoolInfo> schoolMap = schoolInfoSupport.buildSchoolInfoMap(records, UserInfo::getSchoolCode);
        List<UserInfoVO> vos = records.stream()
                .map(user -> {
                    UserInfoVO vo = BeanUtil.copyProperties(user, UserInfoVO.class);
                    schoolInfoSupport.fillSchoolCampusNames(
                            user.getSchoolCode(),
                            user.getCampusCode(),
                            schoolMap,
                            vo::setSchoolName,
                            vo::setCampusName
                    );
                    return vo;
                })
                .toList();

        return new Page<UserInfoVO>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal()).setRecords(vos);
    }

    @Transactional(rollbackFor = Exception.class)
    public void auditRunner(Long operatorId, Long userId, Integer status, String reason) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("审核状态仅支持 1-通过 或 0-驳回");
        }
        String lockKey = "admin:audit:runner:" + userId;
        actionLockSupport.withLock(lockKey, () -> doAuditRunner(operatorId, userId, status, reason));
    }

    private void doAuditRunner(Long operatorId, Long userId, Integer status, String reason) {
        UserInfo user = userInfoMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        iamAccessService.assertCanManageScope(operatorId, user.getSchoolCode(), user.getCampusCode());
        boolean approved = status == 1;
        Integer targetStatus = approved ? RunnableStatus.APPROVED.getCode() : RunnableStatus.REJECTED.getCode();
        Integer currentStatus = user.getRunnableStatus();
        if (targetStatus.equals(currentStatus)) {
            log.info("跑腿员审核重复请求已忽略: operatorId={}, userId={}, runnableStatus={}", operatorId, userId, targetStatus);
            return;
        }
        if (!RunnableStatus.PENDING.getCode().equals(currentStatus)) {
            throw new BusinessException("该跑腿员申请已被处理，请刷新后重试");
        }
        user.setRunnableStatus(targetStatus);
        userInfoMapper.updateById(user);

        String title = approved ? "跑腿员审核通过" : "跑腿员审核未通过";
        String content;
        if (approved) {
            content = "您的跑腿员申请已通过审核，现在可以接单了。";
        } else {
            String rejectReason = StrUtil.isBlank(reason) ? "未通过平台审核规范" : reason;
            content = "您的跑腿员申请未通过审核。原因：" + rejectReason + "。";
        }
        noticeService.sendNotice(userId, title, content, NoticeType.SYSTEM.getCode());
        log.info("跑腿员审核完成: operatorId={}, userId={}, approved={}", operatorId, userId, approved);
    }

    public UserInfoVO getUserDetail(Long operatorId, Long userId) {
        UserInfo user = userInfoMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        iamAccessService.assertCanManageScope(operatorId, user.getSchoolCode(), user.getCampusCode());
        UserInfoVO vo = BeanUtil.copyProperties(user, UserInfoVO.class);
        vo.setCreditScore(creditScoreService.getCreditScore(userId));
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public void adjustUserCreditScore(Long operatorId, Long userId, int change, String reason) {
        UserInfo user = userInfoMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        iamAccessService.assertCanManageScope(operatorId, user.getSchoolCode(), user.getCampusCode());
        creditScoreService.adjustCreditScore(userId, change, reason);
        log.info("管理员调整信用分: operatorId={}, userId={}, change={}, reason={}", operatorId, userId, change, reason);
    }
}
