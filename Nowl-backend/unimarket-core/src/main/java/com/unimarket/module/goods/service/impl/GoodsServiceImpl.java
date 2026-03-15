package com.unimarket.module.goods.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimarket.common.config.RocketMQConfig;
import com.unimarket.common.enums.OrderStatus;
import com.unimarket.common.enums.ReviewStatus;
import com.unimarket.common.enums.TradeStatus;
import com.unimarket.common.exception.BusinessException;
import com.unimarket.common.mq.GoodsAuditMessage;
import com.unimarket.common.mq.GoodsSyncMessage;
import com.unimarket.common.result.PageResult;
import com.unimarket.common.result.ResultCode;
import com.unimarket.common.utils.RedisCache;
import com.unimarket.module.goods.dto.GoodsPublishDTO;
import com.unimarket.module.goods.dto.GoodsQueryDTO;
import com.unimarket.module.goods.entity.CollectionRecord;
import com.unimarket.module.goods.entity.GoodsInfo;
import com.unimarket.module.goods.entity.ItemCategory;
import com.unimarket.module.goods.mapper.CollectionRecordMapper;
import com.unimarket.module.goods.mapper.GoodsInfoMapper;
import com.unimarket.module.goods.mapper.ItemCategoryMapper;
import com.unimarket.module.goods.service.GoodsService;
import com.unimarket.module.goods.vo.GoodsDetailVO;
import com.unimarket.module.goods.vo.GoodsVO;
import com.unimarket.module.iam.service.IamAccessService;
import com.unimarket.module.order.entity.OrderInfo;
import com.unimarket.module.order.mapper.OrderInfoMapper;
import com.unimarket.module.risk.dto.RiskContext;
import com.unimarket.module.risk.enums.RiskEventType;
import com.unimarket.module.risk.service.RiskControlService;
import com.unimarket.module.school.entity.SchoolInfo;
import com.unimarket.module.school.mapper.SchoolInfoMapper;
import com.unimarket.module.user.entity.UserInfo;
import com.unimarket.module.user.mapper.UserInfoMapper;
import com.unimarket.security.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashMap;
import java.util.stream.Collectors;


/**
 * 商品Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsServiceImpl implements GoodsService {

    public static final int WAIT_REVIEW = 0;
    private final GoodsInfoMapper goodsInfoMapper;
    private final CollectionRecordMapper collectionRecordMapper;
    private final SchoolInfoMapper schoolInfoMapper;
    private final ItemCategoryMapper itemCategoryMapper;
    private final UserInfoMapper userInfoMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final RedisCache redisCache;
    private final RocketMQTemplate rocketMQTemplate;
    private final RiskControlService riskControlService;
    private final IamAccessService iamAccessService;

    private static final String GOODS_DETAIL_CACHE_PREFIX = "goods:detail:";
    private static final String NULL_MARKER = "N/A";
    private static final int MAX_PAGE_SIZE = 100;

    @Override
    public PageResult<GoodsVO> list(GoodsQueryDTO dto, Long currentUserId) {
        normalizePageQuery(dto);
        // 构建查询条件
        LambdaQueryWrapper<GoodsInfo> wrapper = new LambdaQueryWrapper<>();

        // 如果学校/校区编码为空（游客），则查询所有商品，否则按学校/校区过滤
        boolean filterBySchool = StrUtil.isNotBlank(dto.getSchoolCode());
        boolean filterByCampus = StrUtil.isNotBlank(dto.getCampusCode());

        if (dto.getCategoryId() != null) {
            wrapper.eq(GoodsInfo::getCategoryId, dto.getCategoryId());
        } else if (dto.getParentCategoryId() != null) {
            LambdaQueryWrapper<ItemCategory> categoryWrapper = new LambdaQueryWrapper<>();
            categoryWrapper.eq(ItemCategory::getParentId, dto.getParentCategoryId());
            List<ItemCategory> childCategories = itemCategoryMapper.selectList(categoryWrapper);
            if (childCategories.isEmpty()) {
                wrapper.eq(GoodsInfo::getCategoryId, dto.getParentCategoryId());
            } else {
                List<Integer> childCategoryIds = childCategories.stream()
                        .map(ItemCategory::getCategoryId)
                        .collect(Collectors.toList());
                wrapper.in(GoodsInfo::getCategoryId, childCategoryIds);
            }
        }

        wrapper.eq(filterBySchool, GoodsInfo::getSchoolCode, dto.getSchoolCode())
                .eq(filterByCampus, GoodsInfo::getCampusCode, dto.getCampusCode())
                .eq(dto.getTradeStatus() != null, GoodsInfo::getTradeStatus, dto.getTradeStatus())
                .eq(dto.getSellerId() != null, GoodsInfo::getSellerId, dto.getSellerId())
                .ge(dto.getMinPrice() != null, GoodsInfo::getPrice, dto.getMinPrice())
                .le(dto.getMaxPrice() != null, GoodsInfo::getPrice, dto.getMaxPrice());

        // 查询集市时，sellerId为null，把所有通过审核的商品都查出来。
        if (dto.getSellerId() == null) {
            wrapper.in(GoodsInfo::getReviewStatus, 1,2);
        }

        // 增强关键词搜索：标题、描述、分类名
        if (StrUtil.isNotBlank(dto.getKeyword())) {
            wrapper.and(w -> w.like(GoodsInfo::getTitle, dto.getKeyword())
                    .or().like(GoodsInfo::getDescription, dto.getKeyword()));
        }

        // 排序逻辑
        if (dto.getSortType() != null) {
            switch (dto.getSortType()) {
                case 1: wrapper.orderByAsc(GoodsInfo::getPrice); break; // 价格升序
                case 2: wrapper.orderByDesc(GoodsInfo::getPrice); break; // 价格降序
                case 3: // 热度（收藏数）
                    wrapper.orderByDesc(GoodsInfo::getCollectCount).orderByDesc(GoodsInfo::getCreateTime);
                    break;
                default: wrapper.orderByDesc(GoodsInfo::getCreateTime); // 默认最新
            }
        } else {
            wrapper.orderByDesc(GoodsInfo::getCreateTime);
        }

        // 分页查询
        Page<GoodsInfo> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        Page<GoodsInfo> result = goodsInfoMapper.selectPage(page, wrapper);

        // 转换为VO
        List<GoodsVO> voList = convertToVOList(result.getRecords(), currentUserId);

        return new PageResult<>(result.getTotal(), voList);
    }

    @Override
    public GoodsDetailVO getDetail(Long productId, Long currentUserId) {
        String cacheKey = buildGoodsDetailCacheKey(productId);
        
        // 1. 检查穿透缓存（空标记）
        if (cacheKey != null) {
            Object marker = redisCache.get(cacheKey);
            if (NULL_MARKER.equals(marker)) {
                log.warn("拦截到针对已缓存空物品ID的查询: {}", productId);
                throw new BusinessException("商品不存在");
            }
        }

        // 查询商品信息
        GoodsInfo goodsInfo = goodsInfoMapper.selectById(productId);
        if (goodsInfo == null) {
            // 2. 写入空值缓存，防止穿透，有效期5分钟
            if (cacheKey != null) {
                // 注意：goods_info 是多租户表，缓存 key 必须带 tenant（schoolCode），避免跨校请求把“空标记”写入导致误伤其他学校。
                redisCache.set(cacheKey, NULL_MARKER, 300);
            }
            throw new BusinessException("商品不存在");
        }

        // 未通过审核的商品不对外展示（仅发布者与具备管理权限的管理员可查看）
        boolean reviewPassed = Objects.equals(goodsInfo.getReviewStatus(), ReviewStatus.AI_PASSED.getCode())
                || Objects.equals(goodsInfo.getReviewStatus(), ReviewStatus.MANUAL_PASSED.getCode());
        boolean isSeller = currentUserId != null && currentUserId.equals(goodsInfo.getSellerId());
        if (!reviewPassed && !isSeller && !canAdminViewGoods(currentUserId, goodsInfo)) {
            throw new BusinessException("商品不存在或未通过审核");
        }

        // 查询卖家信息
        UserInfo seller = userInfoMapper.selectById(goodsInfo.getSellerId());

        // 查询学校信息
        LambdaQueryWrapper<SchoolInfo> schoolWrapper = new LambdaQueryWrapper<>();
        schoolWrapper.eq(SchoolInfo::getSchoolCode, goodsInfo.getSchoolCode())
                .eq(SchoolInfo::getCampusCode, goodsInfo.getCampusCode());
        SchoolInfo schoolInfo = schoolInfoMapper.selectOne(schoolWrapper);

        // 查询分类信息
        ItemCategory category = itemCategoryMapper.selectById(goodsInfo.getCategoryId());

        // 转换为VO
        GoodsDetailVO vo = BeanUtil.copyProperties(goodsInfo, GoodsDetailVO.class);
        if (seller != null) {
            vo.setSellerName(seller.getNickName());
            vo.setSellerAvatar(seller.getImageUrl());
            vo.setSellerAuthStatus(seller.getAuthStatus());
            vo.setSellerCreditScore(seller.getCreditScore());
        }
        if (schoolInfo != null) {
            vo.setSchoolName(schoolInfo.getSchoolName());
            vo.setCampusName(schoolInfo.getCampusName());
        }
        if (category != null) {
            vo.setCategoryName(category.getCategoryName());
        }

        // 查询是否已收藏
        if (currentUserId != null) {
            LambdaQueryWrapper<CollectionRecord> collectionWrapper = new LambdaQueryWrapper<>();
            collectionWrapper.eq(CollectionRecord::getUserId, currentUserId)
                    .eq(CollectionRecord::getProductId, productId);
            Long count = collectionRecordMapper.selectCount(collectionWrapper);
            vo.setIsCollected(count > 0);
        } else {
            vo.setIsCollected(false);
        }

        return vo;
    }

    private String buildGoodsDetailCacheKey(Long productId) {
        if (productId == null) {
            return null;
        }

        if (UserContextHolder.isGuest()) {
            return GOODS_DETAIL_CACHE_PREFIX + "guest:" + productId;
        }

        String schoolCode = UserContextHolder.getSchoolCode();
        if (StrUtil.isBlank(schoolCode)) {
            return null;
        }
        return GOODS_DETAIL_CACHE_PREFIX + schoolCode.trim() + ":" + productId;
    }

    private boolean canAdminViewGoods(Long userId, GoodsInfo goodsInfo) {
        if (userId == null || goodsInfo == null) {
            return false;
        }

        boolean hasPerm = iamAccessService.hasPermission(userId, "admin:goods:list:view")
                || iamAccessService.hasPermission(userId, "admin:goods:pending:view")
                || iamAccessService.hasPermission(userId, "admin:goods:audit");
        if (!hasPerm) {
            return false;
        }

        return iamAccessService.canManageScope(userId, goodsInfo.getSchoolCode(), goodsInfo.getCampusCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long userId, GoodsPublishDTO dto) {
        // 查询用户信息（权限校验已在Controller层通过@PreAuthorize完成）
        UserInfo userInfo = userInfoMapper.selectById(userId);
        if (userInfo == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        Map<String, Object> features = new HashMap<>();
        features.put("categoryId", dto.getCategoryId());
        features.put("price", dto.getPrice());
        features.put("titleLength", dto.getTitle() == null ? 0 : dto.getTitle().length());

        Map<String, Object> rawPayload = new HashMap<>();
        rawPayload.put("title", dto.getTitle());
        rawPayload.put("description", dto.getDescription());
        rawPayload.put("image", dto.getImage());
        rawPayload.put("imageList", dto.getImageList());
        rawPayload.put("categoryId", dto.getCategoryId());
        rawPayload.put("price", dto.getPrice());
        rawPayload.put("userId", userId);

        riskControlService.assertAllowed(RiskContext.builder()
                .eventType(RiskEventType.GOODS_PUBLISH)
                .userId(userId)
                .subjectId(String.valueOf(userId))
                .schoolCode(userInfo.getSchoolCode())
                .campusCode(userInfo.getCampusCode())
                .features(features)
                .rawPayload(rawPayload)
                .build());

        // 创建商品
        GoodsInfo goodsInfo = BeanUtil.copyProperties(dto, GoodsInfo.class);
        goodsInfo.setSellerId(userId);
        goodsInfo.setSchoolCode(userInfo.getSchoolCode());
        goodsInfo.setCampusCode(userInfo.getCampusCode());
        // 审核通过前不对外上架，避免未审核内容被浏览/下单。
        goodsInfo.setTradeStatus(TradeStatus.OFF_SHELF.getCode());
        goodsInfo.setCollectCount(0);

        // 设置为待审核
        goodsInfo.setReviewStatus(ReviewStatus.WAIT_REVIEW.getCode());
        goodsInfo.setAuditReason(null);

        int result = goodsInfoMapper.insert(goodsInfo);
        if (result <= 0) {
            throw new BusinessException("发布商品失败");
        }

        // 确保事务提交后再发送审核消息
        Long goodsId = goodsInfo.getProductId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    GoodsAuditMessage message = GoodsAuditMessage.create(goodsId);
                    log.info("准备发送商品审核消息: goodsId={}, topic={}", goodsId, RocketMQConfig.GOODS_AUDIT_TOPIC);
                    org.apache.rocketmq.client.producer.SendResult sendResult =
                        rocketMQTemplate.syncSend(RocketMQConfig.GOODS_AUDIT_TOPIC, message);
                    log.info("发送商品审核消息成功: goodsId={}, msgId={}, status={}",
                        goodsId, sendResult.getMsgId(), sendResult.getSendStatus());
                } catch (Exception e) {
                    log.error("发送商品审核消息失败: goodsId={}", goodsId, e);
                }
            }
        });

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long userId, Long productId, GoodsPublishDTO dto) {
        // 查询商品
        GoodsInfo goodsInfo = goodsInfoMapper.selectById(productId);
        if (goodsInfo == null) {
            throw new BusinessException(ResultCode.GOODS_NOT_FOUND);
        }

        if (!Objects.equals(goodsInfo.getSellerId(), userId)) {
            throw new BusinessException("无权编辑该商品");
        }

        if (hasNonCancelledOrders(productId)) {
            throw new BusinessException("商品已有有效订单，不能编辑");
        }

        boolean isNormalEditable = Objects.equals(goodsInfo.getTradeStatus(), TradeStatus.ON_SALE.getCode())
                && (Objects.equals(goodsInfo.getReviewStatus(), ReviewStatus.AI_PASSED.getCode())
                || Objects.equals(goodsInfo.getReviewStatus(), ReviewStatus.MANUAL_PASSED.getCode()));
        boolean isRejectedReEditable = Objects.equals(goodsInfo.getReviewStatus(), ReviewStatus.REJECTED.getCode())
                && Objects.equals(goodsInfo.getTradeStatus(), TradeStatus.OFF_SHELF.getCode());

        if (!isNormalEditable && !isRejectedReEditable) {
            throw new BusinessException("当前商品状态不允许编辑");
        }

        // 更新商品信息（注意：重新提审期间不允许对外上架）
        BeanUtil.copyProperties(dto, goodsInfo, "productId", "sellerId", "schoolCode", "campusCode");

        // 设置为待审核
        goodsInfo.setReviewStatus(WAIT_REVIEW);
        goodsInfo.setAuditReason(null);
        goodsInfo.setTradeStatus(TradeStatus.OFF_SHELF.getCode());

        int result = goodsInfoMapper.updateById(goodsInfo);
        if (result <= 0) {
            throw new BusinessException("更新商品失败");
        }

        // 确保事务提交后再发送审核消息
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    rocketMQTemplate.convertAndSend(RocketMQConfig.GOODS_AUDIT_TOPIC,
                            GoodsAuditMessage.update(productId));
                    log.info("发送商品更新审核消息成功: productId={}", productId);
                } catch (Exception e) {
                    log.error("发送商品更新审核消息失败: productId={}", productId, e);
                }
            }
        });

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long productId) {
        // 查询商品
        GoodsInfo goodsInfo = goodsInfoMapper.selectById(productId);
        if (goodsInfo == null) {
            throw new BusinessException("商品不存在");
        }

        if (!Objects.equals(goodsInfo.getSellerId(), userId)) {
            throw new BusinessException("无权删除该商品");
        }

        if (Objects.equals(goodsInfo.getTradeStatus(), TradeStatus.SOLD.getCode())) {
            throw new BusinessException("商品已售出，不能删除");
        }

        if (hasNonCancelledOrders(productId)) {
            throw new BusinessException("商品已有有效订单，不能删除");
        }

        boolean canDelete = Objects.equals(goodsInfo.getTradeStatus(), TradeStatus.OFF_SHELF.getCode())
                || Objects.equals(goodsInfo.getReviewStatus(), ReviewStatus.REJECTED.getCode());
        if (!canDelete) {
            throw new BusinessException("仅下架或驳回状态的商品可删除");
        }

        // 软删除
        int count = goodsInfoMapper.deleteById(productId);
        if (count <= 0) {
            throw new BusinessException("删除商品失败");
        }

        // 同步删除 ES 索引
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    rocketMQTemplate.convertAndSend(RocketMQConfig.GOODS_SYNC_TOPIC,
                            GoodsSyncMessage.deleteMessage(productId));
                    log.info("发送商品删除同步消息成功: productId={}", productId);
                } catch (Exception e) {
                    log.error("发送商品删除消息失败: productId={}", productId, e);
                }
            }
        });

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offshelf(Long userId, Long productId) {
        // 查询商品
        GoodsInfo goodsInfo = goodsInfoMapper.selectById(productId);
        if (goodsInfo == null) {
            throw new BusinessException("商品不存在");
        }

        // 检查商品状态
        if (goodsInfo.getTradeStatus() == 2) {
            throw new BusinessException("商品已下架");
        }
        if (goodsInfo.getTradeStatus() == 1) {
            throw new BusinessException("商品已售出，无法下架");
        }

        // 设置为已下架状态
        goodsInfo.setTradeStatus(TradeStatus.OFF_SHELF.getCode());
        int count = goodsInfoMapper.updateById(goodsInfo);
        if (count <= 0) {
            throw new BusinessException("下架商品失败");
        }

        // 同步更新 ES 索引（状态变更）
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    rocketMQTemplate.convertAndSend(RocketMQConfig.GOODS_SYNC_TOPIC,
                            GoodsSyncMessage.updateMessage(productId));
                    log.info("发送商品下架同步消息成功: productId={}", productId);
                } catch (Exception e) {
                    log.error("发送商品下架同步消息失败: productId={}", productId, e);
                }
            }
        });

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void collect(Long userId, Long productId) {
        // 检查商品是否存在
        GoodsInfo goodsInfo = goodsInfoMapper.selectById(productId);
        if (goodsInfo == null) {
            throw new BusinessException("商品不存在");
        }

        // 检查是否已收藏
        LambdaQueryWrapper<CollectionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CollectionRecord::getUserId, userId)
                .eq(CollectionRecord::getProductId, productId);
        Long count = collectionRecordMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException("已收藏该商品");
        }

        // 创建收藏记录
        CollectionRecord record = new CollectionRecord();
        record.setUserId(userId);
        record.setProductId(productId);
        collectionRecordMapper.insert(record);

        // 更新商品收藏数
        goodsInfo.setCollectCount(goodsInfo.getCollectCount() + 1);
        goodsInfoMapper.updateById(goodsInfo);
        notifyGoodsSearchUpdated(productId, "收藏");

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uncollect(Long userId, Long productId) {
        // 查询收藏记录
        LambdaQueryWrapper<CollectionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CollectionRecord::getUserId, userId)
                .eq(CollectionRecord::getProductId, productId);
        CollectionRecord record = collectionRecordMapper.selectOne(wrapper);
        if (record == null) {
            throw new BusinessException("未收藏该商品");
        }

        // 删除收藏记录
        collectionRecordMapper.deleteById(record.getCollectionId());

        // 更新商品收藏数
        GoodsInfo goodsInfo = goodsInfoMapper.selectById(productId);
        if (goodsInfo != null && goodsInfo.getCollectCount() > 0) {
            goodsInfo.setCollectCount(goodsInfo.getCollectCount() - 1);
            goodsInfoMapper.updateById(goodsInfo);
            notifyGoodsSearchUpdated(productId, "取消收藏");
        }

    }

    private void notifyGoodsSearchUpdated(Long productId, String actionName) {
        if (productId == null) {
            return;
        }
        Runnable syncTask = () -> {
            try {
                rocketMQTemplate.convertAndSend(RocketMQConfig.GOODS_SYNC_TOPIC,
                        GoodsSyncMessage.updateMessage(productId));
                log.info("发送商品{}同步消息成功: productId={}", actionName, productId);
            } catch (Exception e) {
                log.error("发送商品{}同步消息失败: productId={}", actionName, productId, e);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    syncTask.run();
                }
            });
            return;
        }
        syncTask.run();
    }

    @Override
    public PageResult<GoodsVO> getMyGoods(Long userId, GoodsQueryDTO dto) {
        dto.setSellerId(userId);
        return list(dto, userId);
    }

    @Override
    public PageResult<GoodsVO> getMyCollections(Long userId, GoodsQueryDTO dto) {
        normalizePageQuery(dto);
        cleanupInvalidCollections(userId);

        LambdaQueryWrapper<CollectionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CollectionRecord::getUserId, userId)
                .orderByDesc(CollectionRecord::getCreateTime);

        Long total = collectionRecordMapper.selectCount(wrapper);
        if (total == null || total == 0) {
            return new PageResult<>(0L, new ArrayList<>());
        }

        Page<CollectionRecord> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        Page<CollectionRecord> result = collectionRecordMapper.selectPage(page, wrapper);

        if (result.getRecords().isEmpty()) {
            return new PageResult<>(total, new ArrayList<>());
        }

        List<Long> productIds = result.getRecords().stream()
                .map(CollectionRecord::getProductId)
                .collect(Collectors.toList());

        List<GoodsInfo> goodsList = goodsInfoMapper.selectBatchIds(productIds);
        Map<Long, GoodsInfo> goodsMap = goodsList.stream()
                .collect(Collectors.toMap(GoodsInfo::getProductId, goods -> goods));

        List<GoodsInfo> orderedGoodsList = result.getRecords().stream()
                .map(record -> goodsMap.get(record.getProductId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<GoodsVO> voList = convertToVOList(orderedGoodsList, userId);
        return new PageResult<>(total, voList);
    }

    private void normalizePageQuery(GoodsQueryDTO dto) {
        if (dto == null) {
            return;
        }
        Integer pageNum = dto.getPageNum();
        Integer pageSize = dto.getPageSize();
        dto.setPageNum(pageNum == null || pageNum < 1 ? 1 : pageNum);
        if (pageSize == null || pageSize < 1) {
            dto.setPageSize(10);
        } else {
            dto.setPageSize(Math.min(pageSize, MAX_PAGE_SIZE));
        }
    }

    private void cleanupInvalidCollections(Long userId) {
        LambdaQueryWrapper<CollectionRecord> invalidWrapper = new LambdaQueryWrapper<>();
        invalidWrapper.eq(CollectionRecord::getUserId, userId)
                .notInSql(CollectionRecord::getProductId, "select product_id from goods_info where is_deleted = 0");
        collectionRecordMapper.delete(invalidWrapper);

        LambdaQueryWrapper<CollectionRecord> allWrapper = new LambdaQueryWrapper<>();
        allWrapper.eq(CollectionRecord::getUserId, userId)
                .orderByDesc(CollectionRecord::getCreateTime)
                .orderByDesc(CollectionRecord::getCollectionId);
        List<CollectionRecord> allRecords = collectionRecordMapper.selectList(allWrapper);

        Set<Long> seenProductIds = new HashSet<>();
        List<Long> duplicateRecordIds = new ArrayList<>();
        for (CollectionRecord record : allRecords) {
            if (!seenProductIds.add(record.getProductId())) {
                duplicateRecordIds.add(record.getCollectionId());
            }
        }

        if (!duplicateRecordIds.isEmpty()) {
            collectionRecordMapper.deleteByIds(duplicateRecordIds);
        }
    }

    /**
     * 转换为VO列表
     */
    private List<GoodsVO> convertToVOList(List<GoodsInfo> goodsList, Long currentUserId) {
        if (goodsList.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查询卖家信息
        List<Long> sellerIds = goodsList.stream()
                .map(GoodsInfo::getSellerId)
                .distinct()
                .collect(Collectors.toList());
        List<UserInfo> sellers = userInfoMapper.selectBatchIds(sellerIds);
        Map<Long, UserInfo> sellerMap = sellers.stream()
                .collect(Collectors.toMap(UserInfo::getUserId, user -> user));

        // 批量查询分类信息
        List<Integer> categoryIds = goodsList.stream()
                .map(GoodsInfo::getCategoryId)
                .distinct()
                .collect(Collectors.toList());
        List<ItemCategory> categories = itemCategoryMapper.selectBatchIds(categoryIds);
        Map<Integer, ItemCategory> categoryMap = categories.stream()
                .collect(Collectors.toMap(ItemCategory::getCategoryId, category -> category));

        // 批量查询学校/校区名称，商品缺失编码时回退到卖家绑定学校/校区
        Set<String> schoolCodeSet = new HashSet<>();
        Set<String> campusCodeSet = new HashSet<>();
        for (GoodsInfo goods : goodsList) {
            UserInfo seller = sellerMap.get(goods.getSellerId());
            String resolvedSchoolCode = resolveSchoolCode(goods, seller);
            String resolvedCampusCode = resolveCampusCode(goods, seller);
            if (StrUtil.isNotBlank(resolvedSchoolCode) && StrUtil.isNotBlank(resolvedCampusCode)) {
                schoolCodeSet.add(resolvedSchoolCode);
                campusCodeSet.add(resolvedCampusCode);
            }
        }
        Map<String, SchoolInfo> schoolMap = new HashMap<>();
        if (!schoolCodeSet.isEmpty() && !campusCodeSet.isEmpty()) {
            LambdaQueryWrapper<SchoolInfo> schoolWrapper = new LambdaQueryWrapper<>();
            schoolWrapper.in(SchoolInfo::getSchoolCode, schoolCodeSet)
                    .in(SchoolInfo::getCampusCode, campusCodeSet)
                    .eq(SchoolInfo::getStatus, 1);
            List<SchoolInfo> schools = schoolInfoMapper.selectList(schoolWrapper);
            schoolMap = schools.stream().collect(Collectors.toMap(
                    item -> buildSchoolCampusKey(item.getSchoolCode(), item.getCampusCode()),
                    item -> item,
                    (a, b) -> a
            ));
        }

        // 查询收藏状态
        Map<Long, Boolean> collectionMap = null;
        if (currentUserId != null) {
            List<Long> productIds = goodsList.stream()
                    .map(GoodsInfo::getProductId)
                    .collect(Collectors.toList());
            LambdaQueryWrapper<CollectionRecord> collectionWrapper = new LambdaQueryWrapper<>();
            collectionWrapper.eq(CollectionRecord::getUserId, currentUserId)
                    .in(CollectionRecord::getProductId, productIds);
            List<CollectionRecord> collections = collectionRecordMapper.selectList(collectionWrapper);
            collectionMap = collections.stream()
                    .collect(Collectors.toMap(CollectionRecord::getProductId, record -> true));
        }

        // 转换为VO
        List<GoodsVO> voList = new ArrayList<>();
        Map<Long, Boolean> finalCollectionMap = collectionMap;
        for (GoodsInfo goods : goodsList) {
            GoodsVO vo = BeanUtil.copyProperties(goods, GoodsVO.class);

            // 设置卖家信息
            UserInfo seller = sellerMap.get(goods.getSellerId());
            if (seller != null) {
                vo.setSellerName(seller.getNickName());
                vo.setSellerAvatar(seller.getImageUrl());
                vo.setSellerAuthStatus(seller.getAuthStatus());
            }

            // 设置分类名称
            ItemCategory category = categoryMap.get(goods.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getCategoryName());
            }

            String resolvedSchoolCode = resolveSchoolCode(goods, seller);
            String resolvedCampusCode = resolveCampusCode(goods, seller);
            vo.setSchoolCode(resolvedSchoolCode);
            vo.setCampusCode(resolvedCampusCode);

            SchoolInfo schoolInfo = schoolMap.get(buildSchoolCampusKey(resolvedSchoolCode, resolvedCampusCode));
            if (schoolInfo != null) {
                vo.setSchoolName(schoolInfo.getSchoolName());
                vo.setCampusName(schoolInfo.getCampusName());
            } else {
                vo.setSchoolName(resolvedSchoolCode);
                vo.setCampusName(resolvedCampusCode);
            }

            // 设置收藏状态
            vo.setIsCollected(finalCollectionMap != null && finalCollectionMap.getOrDefault(goods.getProductId(), false));

            voList.add(vo);
        }

        return voList;
    }

    private String resolveSchoolCode(GoodsInfo goods, UserInfo seller) {
        if (StrUtil.isNotBlank(goods.getSchoolCode())) {
            return goods.getSchoolCode();
        }
        return seller == null ? null : seller.getSchoolCode();
    }

    private String resolveCampusCode(GoodsInfo goods, UserInfo seller) {
        if (StrUtil.isNotBlank(goods.getCampusCode())) {
            return goods.getCampusCode();
        }
        return seller == null ? null : seller.getCampusCode();
    }

    private String buildSchoolCampusKey(String schoolCode, String campusCode) {
        if (StrUtil.isBlank(schoolCode) || StrUtil.isBlank(campusCode)) {
            return null;
        }
        return schoolCode + "|" + campusCode;
    }

    private boolean hasNonCancelledOrders(Long productId) {
        LambdaQueryWrapper<OrderInfo> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(OrderInfo::getProductId, productId)
                .ne(OrderInfo::getOrderStatus, OrderStatus.CANCELLED.getCode());
        Long count = orderInfoMapper.selectCount(orderWrapper);
        return count != null && count > 0;
    }
}
