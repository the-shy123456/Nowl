package com.unimarket.admin.service;

import com.unimarket.admin.service.impl.domain.AdminUserDomainService;
import com.unimarket.admin.service.impl.support.AdminActionLockSupport;
import com.unimarket.admin.service.impl.support.AdminScopeSupport;
import com.unimarket.admin.service.impl.support.AdminSchoolInfoSupport;
import com.unimarket.common.enums.AuthStatus;
import com.unimarket.common.enums.NoticeType;
import com.unimarket.common.enums.RunnableStatus;
import com.unimarket.common.exception.BusinessException;
import com.unimarket.module.iam.service.IamAccessService;
import com.unimarket.module.notice.service.NoticeService;
import com.unimarket.module.user.entity.UserInfo;
import com.unimarket.module.user.mapper.UserInfoMapper;
import com.unimarket.module.user.service.CreditScoreService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserDomainServiceNoticeTest {

    @Mock
    private UserInfoMapper userInfoMapper;
    @Mock
    private CreditScoreService creditScoreService;
    @Mock
    private NoticeService noticeService;
    @Mock
    private IamAccessService iamAccessService;
    @Mock
    private AdminActionLockSupport actionLockSupport;
    @Mock
    private AdminScopeSupport scopeSupport;
    @Mock
    private AdminSchoolInfoSupport schoolInfoSupport;

    @InjectMocks
    private AdminUserDomainService userDomainService;

    @BeforeEach
    void setUp() {
        lenient().doAnswer(invocation -> {
            Runnable action = invocation.getArgument(1);
            action.run();
            return null;
        }).when(actionLockSupport).withLock(anyString(), any(Runnable.class));
    }

    @Test
    @DisplayName("auditRunner: 通过时更新状态并发送系统通知")
    void auditRunner_approved_sendNotice() {
        UserInfo user = new UserInfo();
        user.setUserId(100L);
        user.setSchoolCode("SC001");
        user.setCampusCode("C001");
        user.setRunnableStatus(RunnableStatus.PENDING.getCode());
        when(userInfoMapper.selectById(100L)).thenReturn(user);
        when(userInfoMapper.updateById(any(UserInfo.class))).thenReturn(1);

        userDomainService.auditRunner(1L, 100L, 1, null);

        ArgumentCaptor<UserInfo> captor = ArgumentCaptor.forClass(UserInfo.class);
        verify(userInfoMapper).updateById(captor.capture());
        assertEquals(RunnableStatus.APPROVED.getCode(), captor.getValue().getRunnableStatus());
        verify(noticeService).sendNotice(
                100L,
                "跑腿员审核通过",
                "您的跑腿员申请已通过审核，现在可以接单了。",
                NoticeType.SYSTEM.getCode()
        );
    }

    @Test
    @DisplayName("auditRunner: 驳回时发送原因")
    void auditRunner_rejected_sendNoticeWithReason() {
        UserInfo user = new UserInfo();
        user.setUserId(100L);
        user.setSchoolCode("SC001");
        user.setCampusCode("C001");
        user.setRunnableStatus(RunnableStatus.PENDING.getCode());
        when(userInfoMapper.selectById(100L)).thenReturn(user);
        when(userInfoMapper.updateById(any(UserInfo.class))).thenReturn(1);

        userDomainService.auditRunner(1L, 100L, 0, "资料不符");

        ArgumentCaptor<UserInfo> captor = ArgumentCaptor.forClass(UserInfo.class);
        verify(userInfoMapper).updateById(captor.capture());
        assertEquals(RunnableStatus.REJECTED.getCode(), captor.getValue().getRunnableStatus());
        verify(noticeService).sendNotice(
                100L,
                "跑腿员审核未通过",
                "您的跑腿员申请未通过审核。原因：资料不符。",
                NoticeType.SYSTEM.getCode()
        );
    }

    @Test
    @DisplayName("auditUserAuth: 通过时更新状态并发送系统通知")
    void auditUserAuth_approved_sendNotice() {
        UserInfo user = new UserInfo();
        user.setUserId(200L);
        user.setSchoolCode("SC001");
        user.setCampusCode("C001");
        user.setAuthStatus(AuthStatus.PENDING.getCode());
        when(userInfoMapper.selectById(200L)).thenReturn(user);
        when(userInfoMapper.updateById(any(UserInfo.class))).thenReturn(1);

        userDomainService.auditUserAuth(1L, 200L, 1, null);

        ArgumentCaptor<UserInfo> captor = ArgumentCaptor.forClass(UserInfo.class);
        verify(userInfoMapper).updateById(captor.capture());
        assertEquals(AuthStatus.APPROVED.getCode(), captor.getValue().getAuthStatus());
        verify(noticeService).sendNotice(
                200L,
                "实名认证审核通过",
                "您的实名认证已通过审核，现在可以正常发布与交易。",
                NoticeType.SYSTEM.getCode()
        );
    }

    @Test
    @DisplayName("auditUserAuth: 驳回时发送原因")
    void auditUserAuth_rejected_sendNoticeWithReason() {
        UserInfo user = new UserInfo();
        user.setUserId(200L);
        user.setSchoolCode("SC001");
        user.setCampusCode("C001");
        user.setAuthStatus(AuthStatus.PENDING.getCode());
        when(userInfoMapper.selectById(200L)).thenReturn(user);
        when(userInfoMapper.updateById(any(UserInfo.class))).thenReturn(1);

        userDomainService.auditUserAuth(1L, 200L, 0, "照片不清晰");

        ArgumentCaptor<UserInfo> captor = ArgumentCaptor.forClass(UserInfo.class);
        verify(userInfoMapper).updateById(captor.capture());
        assertEquals(AuthStatus.REJECTED.getCode(), captor.getValue().getAuthStatus());
        verify(noticeService).sendNotice(
                200L,
                "实名认证审核未通过",
                "您的实名认证未通过审核。原因：照片不清晰。",
                NoticeType.SYSTEM.getCode()
        );
    }

    @Test
    @DisplayName("auditRunner: status非法时抛出异常")
    void auditRunner_invalidStatus_throw() {
        UserInfo user = new UserInfo();
        user.setUserId(100L);
        user.setSchoolCode("SC001");
        user.setCampusCode("C001");

        assertThrows(BusinessException.class, () -> userDomainService.auditRunner(1L, 100L, 9, null));
    }

    @Test
    @DisplayName("auditRunner: 终态同结果请求幂等忽略")
    void auditRunner_terminalSameRequest_idempotentIgnore() {
        UserInfo user = new UserInfo();
        user.setUserId(100L);
        user.setSchoolCode("SC001");
        user.setCampusCode("C001");
        user.setRunnableStatus(RunnableStatus.APPROVED.getCode());
        when(userInfoMapper.selectById(100L)).thenReturn(user);

        userDomainService.auditRunner(1L, 100L, 1, null);

        verify(userInfoMapper, never()).updateById(any(UserInfo.class));
        verify(noticeService, never()).sendNotice(anyLong(), anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("auditUserAuth: 已处理后冲突请求抛出异常")
    void auditUserAuth_terminalConflict_throw() {
        UserInfo user = new UserInfo();
        user.setUserId(200L);
        user.setSchoolCode("SC001");
        user.setCampusCode("C001");
        user.setAuthStatus(AuthStatus.APPROVED.getCode());
        when(userInfoMapper.selectById(200L)).thenReturn(user);

        assertThrows(BusinessException.class, () -> userDomainService.auditUserAuth(1L, 200L, 0, "改判"));
    }
}
