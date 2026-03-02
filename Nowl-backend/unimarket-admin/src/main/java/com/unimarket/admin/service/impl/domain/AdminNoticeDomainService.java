package com.unimarket.admin.service.impl.domain;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unimarket.admin.service.impl.support.AdminScopeSupport;
import com.unimarket.common.enums.NoticeType;
import com.unimarket.common.exception.BusinessException;
import com.unimarket.module.iam.entity.IamAdminScopeBinding;
import com.unimarket.module.iam.service.IamAccessService;
import com.unimarket.module.notice.service.NoticeService;
import com.unimarket.module.user.entity.UserInfo;
import com.unimarket.module.user.mapper.UserInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNoticeDomainService {

    private final UserInfoMapper userInfoMapper;
    private final NoticeService noticeService;
    private final IamAccessService iamAccessService;
    private final AdminScopeSupport scopeSupport;

    public void broadcastNotice(Long operatorId, String title, String content, String schoolCode, String campusCode) {
        List<IamAdminScopeBinding> scopes = scopeSupport.getOperatorScopes(operatorId);
        String normalizedTitle = StrUtil.trim(title);
        String normalizedContent = StrUtil.trim(content);
        String normalizedSchoolCode = StrUtil.trim(schoolCode);
        String normalizedCampusCode = StrUtil.trim(campusCode);

        if (StrUtil.isBlank(normalizedTitle)) {
            throw new BusinessException("通知标题不能为空");
        }
        if (StrUtil.isBlank(normalizedContent)) {
            throw new BusinessException("通知内容不能为空");
        }
        if (StrUtil.isBlank(normalizedSchoolCode) && StrUtil.isNotBlank(normalizedCampusCode)) {
            throw new BusinessException("指定校区广播时必须同时指定学校编码");
        }
        if (StrUtil.isNotBlank(normalizedSchoolCode)) {
            iamAccessService.assertCanManageScope(operatorId, normalizedSchoolCode, normalizedCampusCode);
        }

        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<UserInfo>()
                .eq(UserInfo::getAccountStatus, 0)
                .eq(StrUtil.isNotBlank(normalizedSchoolCode), UserInfo::getSchoolCode, normalizedSchoolCode)
                .eq(StrUtil.isNotBlank(normalizedCampusCode), UserInfo::getCampusCode, normalizedCampusCode);
        scopeSupport.applyScopeFilter(wrapper, scopes, UserInfo::getSchoolCode, UserInfo::getCampusCode);

        List<UserInfo> users = userInfoMapper.selectList(wrapper);
        for (UserInfo user : users) {
            noticeService.sendNotice(user.getUserId(), normalizedTitle, normalizedContent, NoticeType.SYSTEM.getCode());
        }
        log.info("系统通知广播完成: operatorId={}, schoolCode={}, campusCode={}, userCount={}",
                operatorId, normalizedSchoolCode, normalizedCampusCode, users.size());
    }
}

