package com.unimarket.module.errand.service;

import com.unimarket.common.enums.ErrandStatus;
import com.unimarket.module.errand.entity.ErrandTask;
import com.unimarket.module.errand.mapper.ErrandTaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 跑腿权限校验服务
 * 用于 @PreAuthorize 注解中的权限判断
 *
 * 使用方式：@PreAuthorize("@errandPermission.isPublisher(#taskId)")
 */
@Slf4j
@Service("errandPermission")
@RequiredArgsConstructor
public class ErrandPermissionService {

    private final ErrandTaskMapper errandTaskMapper;

    /**
     * 判断当前用户是否为任务发布者
     *
     * @param taskId 任务ID
     * @param userId 用户ID（从 authentication.principal.userId 获取）
     * @return 是否为发布者
     */
    public boolean isPublisher(Long taskId, Long userId) {
        if (taskId == null || userId == null) {
            return false;
        }
        ErrandTask task = errandTaskMapper.selectById(taskId);
        if (task == null) {
            log.warn("跑腿任务不存在: taskId={}", taskId);
            return false;
        }
        return task.getPublisherId().equals(userId);
    }

    /**
     * 判断当前用户是否为任务接单人
     *
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 是否为接单人
     */
    public boolean isAcceptor(Long taskId, Long userId) {
        if (taskId == null || userId == null) {
            return false;
        }
        ErrandTask task = errandTaskMapper.selectById(taskId);
        if (task == null) {
            log.warn("跑腿任务不存在: taskId={}", taskId);
            return false;
        }
        return task.getAcceptorId() != null && task.getAcceptorId().equals(userId);
    }

    /**
     * 判断当前用户是否为任务参与者（发布者或接单人）
     *
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 是否为参与者
     */
    public boolean isParticipant(Long taskId, Long userId) {
        if (taskId == null || userId == null) {
            return false;
        }
        ErrandTask task = errandTaskMapper.selectById(taskId);
        if (task == null) {
            log.warn("跑腿任务不存在: taskId={}", taskId);
            return false;
        }
        return task.getPublisherId().equals(userId)
                || (task.getAcceptorId() != null && task.getAcceptorId().equals(userId));
    }

    /**
     * 判断当前用户是否可以取消任务
     * - 待接单状态：仅发布者可取消
     * - 进行中状态：发布者可取消；接单人“取消”将被视为放弃接单并重新上架
     *
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 是否可以取消
     */
    public boolean canCancel(Long taskId, Long userId) {
        if (taskId == null || userId == null) {
            return false;
        }
        ErrandTask task = errandTaskMapper.selectById(taskId);
        if (task == null) {
            log.warn("跑腿任务不存在: taskId={}", taskId);
            return false;
        }

        Integer status = task.getTaskStatus();
        boolean isPublisher = task.getPublisherId().equals(userId);
        boolean isAcceptor = task.getAcceptorId() != null && task.getAcceptorId().equals(userId);

        // 待接单状态：仅发布者可取消
        if (ErrandStatus.PENDING.getCode().equals(status)) {
            return isPublisher;
        }

        // 进行中状态：发布者和接单人都可取消
        if (ErrandStatus.IN_PROGRESS.getCode().equals(status)) {
            return isPublisher || isAcceptor;
        }

        // 其他状态不允许取消
        return false;
    }

    /**
     * 判断当前用户是否可以确认完成（仅发布者且任务在待确认状态）
     *
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 是否可以确认
     */
    public boolean canConfirm(Long taskId, Long userId) {
        if (taskId == null || userId == null) {
            return false;
        }
        ErrandTask task = errandTaskMapper.selectById(taskId);
        if (task == null) {
            return false;
        }
        return task.getPublisherId().equals(userId)
                && ErrandStatus.PENDING_CONFIRM.getCode().equals(task.getTaskStatus());
    }

    /**
     * 判断当前用户是否可以送达（仅接单人且任务在进行中状态）
     *
     * @param taskId 任务ID
     * @param userId 用户ID
     * @return 是否可以送达
     */
    public boolean canDeliver(Long taskId, Long userId) {
        if (taskId == null || userId == null) {
            return false;
        }
        ErrandTask task = errandTaskMapper.selectById(taskId);
        if (task == null) {
            return false;
        }
        return task.getAcceptorId() != null
                && task.getAcceptorId().equals(userId)
                && ErrandStatus.IN_PROGRESS.getCode().equals(task.getTaskStatus());
    }

    /**
     * 判断当前用户是否可以编辑任务（仅发布者且待接单）
     */
    public boolean canEdit(Long taskId, Long userId) {
        if (taskId == null || userId == null) {
            return false;
        }
        ErrandTask task = errandTaskMapper.selectById(taskId);
        if (task == null) {
            return false;
        }
        return task.getPublisherId().equals(userId)
                && ErrandStatus.PENDING.getCode().equals(task.getTaskStatus());
    }
}
