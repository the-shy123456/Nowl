package com.unimarket.module.errand.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimarket.common.result.PageQuery;
import com.unimarket.module.errand.dto.ErrandPublishDTO;
import com.unimarket.module.errand.dto.ErrandQueryDTO;
import com.unimarket.module.errand.dto.LocationUploadDTO;
import com.unimarket.module.errand.vo.ErrandVO;

/**
 * 跑腿任务Service接口
 */
public interface ErrandService {

    /**
     * 发布跑腿任务
     */
    void publishErrand(Long userId, ErrandPublishDTO dto);

    /**
     * 查询跑腿任务列表
     */
    Page<ErrandVO> getErrandList(ErrandQueryDTO dto);

    /**
     * 查询跑腿任务详情
     */
    ErrandVO getErrandDetail(Long taskId);

    /**
     * 修改跑腿任务（仅发布者且待接单状态）
     */
    void updateErrand(Long userId, Long taskId, ErrandPublishDTO dto);

    /**
     * 接单
     */
    void acceptErrand(Long userId, Long taskId);

    /**
     * 送达任务（接单人上传凭证）
     * 状态从"进行中"变为"待确认"
     * 权限验证由 @PreAuthorize 在 Controller 层完成
     */
    void deliverErrand(Long taskId, String evidenceImage);

    /**
     * 确认完成（发布者确认）
     * 状态从"待确认"变为"已完成"，结算佣金
     * 权限验证由 @PreAuthorize 在 Controller 层完成
     */
    void confirmErrand(Long taskId);

    /**
     * 取消任务
     * 权限验证由 @PreAuthorize 在 Controller 层完成
     */
    void cancelErrand(Long taskId, String reason);

    /**
     * 查询我发布的跑腿任务
     */
    Page<ErrandVO> getMyPublishedErrands(Long userId, PageQuery query);

    /**
     * 查询我接的跑腿任务
     */
    Page<ErrandVO> getMyAcceptedErrands(Long userId, PageQuery query);

    /**
     * 上报位置
     */
    void uploadLocation(Long userId, LocationUploadDTO dto);

    /**
     * 获取实时位置
     */
    String getLocation(Long taskId);
}
