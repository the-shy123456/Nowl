package com.unimarket.module.dispute.service;

import com.unimarket.module.dispute.entity.DisputeRecord;
import com.unimarket.module.dispute.mapper.DisputeRecordMapper;
import com.unimarket.module.iam.service.IamAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 纠纷权限服务
 */
@Service
@RequiredArgsConstructor
public class DisputePermissionService {

    private static final String PERMISSION_ADMIN_DISPUTE_VIEW = "admin:dispute:list:view";

    private final DisputeRecordMapper disputeRecordMapper;
    private final IamAccessService iamAccessService;

    /**
     * 检查用户是否可查看纠纷（参与方或具备纠纷查看权限的管理员且在管辖范围内）
     */
    public boolean canView(Long userId, Long recordId) {
        DisputeRecord record = selectRecord(recordId);
        return canView(userId, record);
    }

    public boolean canView(Long userId, DisputeRecord record) {
        if (record == null || userId == null) {
            return false;
        }

        if (userId.equals(record.getInitiatorId()) || userId.equals(record.getRelatedId())) {
            return true;
        }

        if (!iamAccessService.hasPermission(userId, PERMISSION_ADMIN_DISPUTE_VIEW)) {
            return false;
        }

        return iamAccessService.canManageScope(userId, record.getSchoolCode(), record.getCampusCode());
    }

    /**
     * 检查用户是否是纠纷参与方
     * @param userId 用户ID
     * @param recordId 纠纷记录ID
     * @return 是否是参与方
     */
    public boolean isParticipant(Long userId, Long recordId) {
        DisputeRecord record = selectRecord(recordId);
        if (record == null || userId == null) {
            return false;
        }
        return userId.equals(record.getInitiatorId()) || userId.equals(record.getRelatedId());
    }

    /**
     * 检查用户是否是纠纷发起人
     * @param userId 用户ID
     * @param recordId 纠纷记录ID
     * @return 是否是发起人
     */
    public boolean isInitiator(Long userId, Long recordId) {
        DisputeRecord record = selectRecord(recordId);
        if (record == null || userId == null) {
            return false;
        }
        return userId.equals(record.getInitiatorId());
    }

    /**
     * 检查纠纷是否可以撤回
     * @param userId 用户ID
     * @param recordId 纠纷记录ID
     * @return 是否可撤回
     */
    public boolean canWithdraw(Long userId, Long recordId) {
        DisputeRecord record = selectRecord(recordId);
        if (record == null || userId == null) {
            return false;
        }
        // 只有发起人可以撤回，且状态为待处理或处理中
        if (!userId.equals(record.getInitiatorId())) {
            return false;
        }
        return record.getHandleStatus() == 0 || record.getHandleStatus() == 1;
    }

    private DisputeRecord selectRecord(Long recordId) {
        if (recordId == null) {
            return null;
        }
        return disputeRecordMapper.selectById(recordId);
    }
}
