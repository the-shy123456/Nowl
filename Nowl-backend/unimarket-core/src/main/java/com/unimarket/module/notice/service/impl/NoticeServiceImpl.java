package com.unimarket.module.notice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.unimarket.common.enums.NoticeType;
import com.unimarket.module.chat.websocket.ChatWebSocketServer;
import com.unimarket.module.errand.entity.ErrandTask;
import com.unimarket.module.errand.mapper.ErrandTaskMapper;
import com.unimarket.module.goods.entity.GoodsInfo;
import com.unimarket.module.goods.mapper.GoodsInfoMapper;
import com.unimarket.module.notice.entity.Notice;
import com.unimarket.module.notice.mapper.NoticeMapper;
import com.unimarket.module.notice.service.NoticeService;
import com.unimarket.module.notice.vo.NoticeVO;
import com.unimarket.module.order.entity.OrderInfo;
import com.unimarket.module.order.mapper.OrderInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private static final String BIZ_SYSTEM = "system";
    private static final String BIZ_PRODUCT = "product";
    private static final String BIZ_ORDER = "order";
    private static final String BIZ_ERRAND = "errand";
    private static final String BIZ_DISPUTE = "dispute";
    private static final String BIZ_REVIEW = "review";

    private final NoticeMapper noticeMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final ErrandTaskMapper errandTaskMapper;
    private final GoodsInfoMapper goodsInfoMapper;

    @Override
    public void sendNotice(Long userId, String title, String content, Integer type) {
        sendNotice(userId, title, content, type, null);
    }

    @Override
    public void sendNotice(Long userId, String title, String content, Integer type, Long relatedId) {
        Notice notice = new Notice();
        notice.setUserId(userId);
        notice.setTitle(title);
        notice.setContent(content);
        notice.setType(type);
        notice.setRelatedId(relatedId);
        notice.setIsRead(0);
        notice.setCreateTime(LocalDateTime.now());
        noticeMapper.insert(notice);

        // 通过 WebSocket 实时推送通知
        try {
            Map<String, Object> wsMsg = new java.util.HashMap<>();
            wsMsg.put("type", "NOTICE");
            wsMsg.put("title", title);
            ChatWebSocketServer.sendMessage(userId, wsMsg);
        } catch (Exception e) {
            log.warn("WebSocket推送通知失败: userId={}", userId, e);
        }
    }

    @Override
    public List<NoticeVO> getMyNotices(Long userId) {
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notice::getUserId, userId).orderByDesc(Notice::getCreateTime);
        List<Notice> notices = noticeMapper.selectList(wrapper);
        Map<Long, OrderInfo> orderMap = resolveOrderMap(notices);
        Map<Long, ErrandTask> errandMap = resolveErrandMap(notices);
        Map<Long, GoodsInfo> goodsMap = resolveGoodsMap(notices);
        return notices.stream()
                .map(notice -> toNoticeVO(notice, userId, orderMap, errandMap, goodsMap))
                .collect(Collectors.toList());
    }

    @Override
    public void markAsRead(Long userId, Long noticeId) {
        LambdaUpdateWrapper<Notice> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Notice::getNoticeId, noticeId)
                .eq(Notice::getUserId, userId)
                .set(Notice::getIsRead, 1);
        noticeMapper.update(null, wrapper);
    }

    @Override
    public void markAllAsRead(Long userId) {
        LambdaUpdateWrapper<Notice> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Notice::getUserId, userId)
                .eq(Notice::getIsRead, 0)
                .set(Notice::getIsRead, 1);
        noticeMapper.update(null, wrapper);
    }

    @Override
    public long getUnreadCount(Long userId) {
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notice::getUserId, userId).eq(Notice::getIsRead, 0);
        return noticeMapper.selectCount(wrapper);
    }

    private Map<Long, OrderInfo> resolveOrderMap(List<Notice> notices) {
        Set<Long> relatedIds = collectTradeRelatedIds(notices);
        if (relatedIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<OrderInfo> orders = orderInfoMapper.selectBatchIds(relatedIds);
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyMap();
        }
        return orders.stream().collect(Collectors.toMap(OrderInfo::getOrderId, order -> order, (a, b) -> a, HashMap::new));
    }

    private Map<Long, ErrandTask> resolveErrandMap(List<Notice> notices) {
        Set<Long> relatedIds = collectTradeRelatedIds(notices);
        if (relatedIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ErrandTask> errands = errandTaskMapper.selectBatchIds(relatedIds);
        if (errands == null || errands.isEmpty()) {
            return Collections.emptyMap();
        }
        return errands.stream().collect(Collectors.toMap(ErrandTask::getTaskId, task -> task, (a, b) -> a, HashMap::new));
    }

    private Map<Long, GoodsInfo> resolveGoodsMap(List<Notice> notices) {
        Set<Long> relatedIds = notices.stream()
                .map(Notice::getRelatedId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (relatedIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<GoodsInfo> goodsList = goodsInfoMapper.selectBatchIds(relatedIds);
        if (goodsList == null || goodsList.isEmpty()) {
            return Collections.emptyMap();
        }
        return goodsList.stream().collect(Collectors.toMap(GoodsInfo::getProductId, goods -> goods, (a, b) -> a, HashMap::new));
    }

    private Set<Long> collectTradeRelatedIds(List<Notice> notices) {
        return notices.stream()
                .filter(notice -> NoticeType.TRADE.getCode().equals(notice.getType()))
                .map(Notice::getRelatedId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
    }

    private NoticeVO toNoticeVO(
            Notice notice,
            Long userId,
            Map<Long, OrderInfo> orderMap,
            Map<Long, ErrandTask> errandMap,
            Map<Long, GoodsInfo> goodsMap
    ) {
        NoticeVO vo = new NoticeVO();
        vo.setNoticeId(notice.getNoticeId());
        vo.setTitle(notice.getTitle());
        vo.setContent(notice.getContent());
        vo.setType(notice.getType());
        vo.setRelatedId(notice.getRelatedId());
        vo.setBizType(resolveBizType(notice, userId, orderMap, errandMap, goodsMap));
        vo.setIsRead(notice.getIsRead());
        vo.setCreateTime(notice.getCreateTime());
        return vo;
    }

    private String resolveBizType(
            Notice notice,
            Long userId,
            Map<Long, OrderInfo> orderMap,
            Map<Long, ErrandTask> errandMap,
            Map<Long, GoodsInfo> goodsMap
    ) {
        Integer type = notice.getType();
        Long relatedId = notice.getRelatedId();
        if (NoticeType.DISPUTE.getCode().equals(type)) {
            return BIZ_DISPUTE;
        }
        if (NoticeType.REVIEW.getCode().equals(type)) {
            return BIZ_REVIEW;
        }
        if (NoticeType.SYSTEM.getCode().equals(type)) {
            return relatedId != null && goodsMap.containsKey(relatedId) ? BIZ_PRODUCT : BIZ_SYSTEM;
        }
        if (!NoticeType.TRADE.getCode().equals(type)) {
            return BIZ_SYSTEM;
        }
        if (relatedId == null || relatedId <= 0) {
            return BIZ_ORDER;
        }

        OrderInfo order = orderMap.get(relatedId);
        ErrandTask task = errandMap.get(relatedId);
        GoodsInfo goods = goodsMap.get(relatedId);

        boolean orderRelated = order != null && (userId.equals(order.getBuyerId()) || userId.equals(order.getSellerId()));
        boolean errandRelated = task != null && (userId.equals(task.getPublisherId()) || userId.equals(task.getAcceptorId()));
        boolean goodsRelated = goods != null && userId.equals(goods.getSellerId());

        if (errandRelated && !orderRelated && !goodsRelated) {
            return BIZ_ERRAND;
        }
        if (orderRelated && !errandRelated && !goodsRelated) {
            return BIZ_ORDER;
        }
        if (goodsRelated && !orderRelated && !errandRelated) {
            return BIZ_PRODUCT;
        }
        if (errandRelated) {
            return BIZ_ERRAND;
        }
        if (orderRelated) {
            return BIZ_ORDER;
        }
        if (goodsRelated) {
            return BIZ_PRODUCT;
        }

        if (task != null && order == null && goods == null) {
            return BIZ_ERRAND;
        }
        if (order != null && task == null && goods == null) {
            return BIZ_ORDER;
        }
        if (goods != null && order == null && task == null) {
            return BIZ_PRODUCT;
        }
        return BIZ_ORDER;
    }
}
