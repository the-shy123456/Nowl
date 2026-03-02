package com.unimarket.admin.service;

import com.unimarket.admin.service.impl.domain.AdminNoticeDomainService;
import com.unimarket.common.enums.NoticeType;
import com.unimarket.common.exception.BusinessException;
import com.unimarket.admin.service.impl.support.AdminScopeSupport;
import com.unimarket.module.iam.entity.IamAdminScopeBinding;
import com.unimarket.module.iam.service.IamAccessService;
import com.unimarket.module.notice.service.NoticeService;
import com.unimarket.module.user.entity.UserInfo;
import com.unimarket.module.user.mapper.UserInfoMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplBroadcastTest {

    @Mock
    private UserInfoMapper userInfoMapper;
    @Mock
    private NoticeService noticeService;
    @Mock
    private IamAccessService iamAccessService;
    @Mock
    private AdminScopeSupport scopeSupport;

    @InjectMocks
    private AdminNoticeDomainService noticeDomainService;

    @Test
    @DisplayName("broadcastNotice: 指定校区但未指定学校时拒绝")
    void broadcastNotice_rejectCampusOnly() {
        IamAdminScopeBinding scope = new IamAdminScopeBinding();
        scope.setScopeType("ALL");
        when(scopeSupport.getOperatorScopes(1L)).thenReturn(List.of(scope));

        assertThrows(BusinessException.class, () ->
                noticeDomainService.broadcastNotice(1L, "通知标题", "通知内容", null, "C001"));
    }

    @Test
    @DisplayName("broadcastNotice: 指定学校时校验管理员可管辖范围并下发通知")
    void broadcastNotice_verifyScopeAndSend() {
        IamAdminScopeBinding scope = new IamAdminScopeBinding();
        scope.setScopeType("SCHOOL");
        scope.setSchoolCode("SC001");
        when(scopeSupport.getOperatorScopes(2L)).thenReturn(List.of(scope));

        UserInfo user1 = new UserInfo();
        user1.setUserId(1001L);
        UserInfo user2 = new UserInfo();
        user2.setUserId(1002L);
        when(userInfoMapper.selectList(any())).thenReturn(List.of(user1, user2));

        noticeDomainService.broadcastNotice(2L, "系统维护", "今晚22点维护", "SC001", null);

        verify(iamAccessService).assertCanManageScope(2L, "SC001", null);
        verify(noticeService, times(2)).sendNotice(any(), eq("系统维护"), eq("今晚22点维护"), eq(NoticeType.SYSTEM.getCode()));
    }
}
