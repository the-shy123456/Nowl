package com.unimarket.module.dispute.service;

import com.unimarket.common.result.PageResult;
import com.unimarket.module.dispute.dto.DisputeCreateDTO;
import com.unimarket.module.dispute.dto.DisputeReplyDTO;
import com.unimarket.module.dispute.vo.DisputeDetailVO;
import com.unimarket.module.dispute.vo.DisputeListItemVO;

/**
 * 纠纷服务接口
 */
public interface DisputeService {

    /**
     * 发起纠纷
     * @param userId 用户ID
     * @param dto 纠纷创建DTO
     */
    void createDispute(Long userId, DisputeCreateDTO dto);

    /**
     * 获取我的纠纷列表
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param handleStatus 处理状态（可选）
     * @return 分页结果
     */
    PageResult<DisputeListItemVO> getMyDisputes(Long userId, Integer pageNum, Integer pageSize, Integer handleStatus);

    /**
     * 获取纠纷详情
     * @param userId 用户ID
     * @param recordId 纠纷记录ID
     * @return 纠纷详情
     */
    DisputeDetailVO getDisputeDetail(Long userId, Long recordId);

    /**
     * 撤回纠纷
     * @param userId 用户ID
     * @param recordId 纠纷记录ID
     */
    void withdrawDispute(Long userId, Long recordId);

    /**
     * 补充证据
     * @param userId 用户ID
     * @param dto 补充证据DTO
     */
    void addEvidence(Long userId, DisputeReplyDTO dto);
}
