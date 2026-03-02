package com.unimarket.search.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimarket.module.goods.entity.GoodsInfo;
import com.unimarket.module.goods.entity.ItemCategory;
import com.unimarket.module.goods.mapper.GoodsInfoMapper;
import com.unimarket.module.goods.mapper.ItemCategoryMapper;
import com.unimarket.module.user.entity.UserInfo;
import com.unimarket.module.user.mapper.UserInfoMapper;
import com.unimarket.search.document.GoodsDocument;
import com.unimarket.search.repository.GoodsSearchRepository;
import com.unimarket.search.service.SearchSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 搜索数据同步服务实现
 * 由 MQ 消费者调用，不再使用 @Async
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchSyncServiceImpl implements SearchSyncService {

    private final GoodsSearchRepository goodsSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final GoodsInfoMapper goodsInfoMapper;
    private final UserInfoMapper userInfoMapper;
    private final ItemCategoryMapper itemCategoryMapper;

    @Override
    public void syncGoods(Long productId) {
        doSyncGoods(productId);
    }

    private void doSyncGoods(Long productId) {
        try {
            GoodsInfo goods = goodsInfoMapper.selectById(productId);
            if (goods == null) {
                log.warn("商品不存在，跳过同步: productId={}", productId);
                return;
            }

            GoodsDocument document = convertToDocument(goods);
            goodsSearchRepository.save(document);
            log.info("商品同步到ES成功: productId={}", productId);
        } catch (Exception e) {
            log.error("商品同步到ES失败: productId={}", productId, e);
        }
    }

    @Override
    public void syncGoodsBatch(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return;
        }

        try {
            List<GoodsInfo> goodsList = goodsInfoMapper.selectBatchIds(productIds);
            if (goodsList.isEmpty()) {
                return;
            }

            List<GoodsDocument> documents = convertToDocuments(goodsList);
            goodsSearchRepository.saveAll(documents);
            log.info("批量同步商品到ES成功: count={}", documents.size());
        } catch (Exception e) {
            log.error("批量同步商品到ES失败", e);
        }
    }

    @Override
    public void deleteGoods(Long productId) {
        try {
            goodsSearchRepository.deleteById(productId);
            log.info("从ES删除商品成功: productId={}", productId);
        } catch (Exception e) {
            log.error("从ES删除商品失败: productId={}", productId, e);
        }
    }

    @Override
    public void updateHotScore(Long productId, Double hotScore) {
        try {
            goodsSearchRepository.findById(productId).ifPresent(doc -> {
                doc.setHotScore(hotScore);
                goodsSearchRepository.save(doc);
                log.debug("更新商品热度分成功: productId={}, hotScore={}", productId, hotScore);
            });
        } catch (Exception e) {
            log.error("更新商品热度分失败: productId={}", productId, e);
        }
    }

    @Override
    public void updateViewCount(Long productId, Integer viewCount) {
        try {
            goodsSearchRepository.findById(productId).ifPresent(doc -> {
                doc.setViewCount(viewCount);
                goodsSearchRepository.save(doc);
            });
        } catch (Exception e) {
            log.error("更新商品浏览量失败: productId={}", productId, e);
        }
    }

    @Override
    public void fullSync() {
        log.info("开始全量同步商品到ES...");

        try {
            // 使用真正的数据库分页查询，防止 OOM
            int pageSize = 500;
            int pageNum = 1;
            long totalSynced = 0;

            while (true) {
                Page<GoodsInfo> page = new Page<>(pageNum, pageSize);
                LambdaQueryWrapper<GoodsInfo> wrapper = new LambdaQueryWrapper<>();
                // reviewStatus: 1-AI通过, 2-人工通过
                wrapper.in(GoodsInfo::getReviewStatus, 1, 2) 
                       .orderByAsc(GoodsInfo::getProductId);

                Page<GoodsInfo> goodsPage = goodsInfoMapper.selectPage(page, wrapper);
                List<GoodsInfo> goodsList = goodsPage.getRecords();
                
                if (goodsList.isEmpty()) {
                    break;
                }

                // 批量转换并保存
                List<GoodsDocument> documents = convertToDocuments(goodsList);
                goodsSearchRepository.saveAll(documents);

                totalSynced += documents.size();
                log.info("全量同步进度: 已同步 {}/{} 条", totalSynced, goodsPage.getTotal());

                if (!goodsPage.hasNext()) {
                    break;
                }
                pageNum++;
            }

            log.info("全量同步完成，共同步{}条商品", totalSynced);
        } catch (Exception e) {
            log.error("全量同步失败", e);
        }
    }

    @Override
    public void createOrUpdateIndex() {
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(GoodsDocument.class);

            if (indexOps.exists()) {
                indexOps.delete();
                log.info("已删除旧的ES索引: goods（将使用正确的分词器设置重建）");
            }

            indexOps.create();
            indexOps.putMapping();
            log.info("ES索引创建成功: goods（已应用ik/pinyin分词器设置）");
        } catch (Exception e) {
            log.error("创建ES索引失败", e);
        }
    }

    /**
     * 转换为ES文档
     */
    private GoodsDocument convertToDocument(GoodsInfo goods) {
        GoodsDocument document = new GoodsDocument();
        BeanUtil.copyProperties(goods, document);

        // 查询卖家信息
        UserInfo seller = userInfoMapper.selectById(goods.getSellerId());
        if (seller != null) {
            document.setSellerName(seller.getNickName());
            document.setSellerAvatar(seller.getImageUrl());
            document.setSellerAuthStatus(seller.getAuthStatus());
            if (StrUtil.isBlank(document.getSchoolCode())) {
                document.setSchoolCode(seller.getSchoolCode());
            }
            if (StrUtil.isBlank(document.getCampusCode())) {
                document.setCampusCode(seller.getCampusCode());
            }
        }

        // 查询分类信息
        ItemCategory category = itemCategoryMapper.selectById(goods.getCategoryId());
        if (category != null) {
            document.setCategoryName(category.getCategoryName());
        }

        // 初始化浏览量和热度分（兜底使用收藏/浏览数据计算，避免热度全为0导致排序退化）
        int viewCount = document.getViewCount() == null ? 0 : document.getViewCount();
        int collectCount = document.getCollectCount() == null ? 0 : document.getCollectCount();
        document.setViewCount(viewCount);
        if (document.getHotScore() == null || document.getHotScore() <= 0) {
            document.setHotScore(viewCount * 1.0 + collectCount * 3.0);
        }

        return document;
    }

    /**
     * 批量转换为ES文档
     */
    private List<GoodsDocument> convertToDocuments(List<GoodsInfo> goodsList) {
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
            .collect(Collectors.toMap(UserInfo::getUserId, s -> s));

        // 批量查询分类信息
        List<Integer> categoryIds = goodsList.stream()
            .map(GoodsInfo::getCategoryId)
            .distinct()
            .collect(Collectors.toList());
        List<ItemCategory> categories = itemCategoryMapper.selectBatchIds(categoryIds);
        Map<Integer, ItemCategory> categoryMap = categories.stream()
            .collect(Collectors.toMap(ItemCategory::getCategoryId, c -> c));

        // 转换
        return goodsList.stream().map(goods -> {
            GoodsDocument document = new GoodsDocument();
            BeanUtil.copyProperties(goods, document);

            UserInfo seller = sellerMap.get(goods.getSellerId());
            if (seller != null) {
                document.setSellerName(seller.getNickName());
                document.setSellerAvatar(seller.getImageUrl());
                document.setSellerAuthStatus(seller.getAuthStatus());
                if (StrUtil.isBlank(document.getSchoolCode())) {
                    document.setSchoolCode(seller.getSchoolCode());
                }
                if (StrUtil.isBlank(document.getCampusCode())) {
                    document.setCampusCode(seller.getCampusCode());
                }
            }

            ItemCategory category = categoryMap.get(goods.getCategoryId());
            if (category != null) {
                document.setCategoryName(category.getCategoryName());
            }

            int viewCount = document.getViewCount() == null ? 0 : document.getViewCount();
            int collectCount = document.getCollectCount() == null ? 0 : document.getCollectCount();
            document.setViewCount(viewCount);
            if (document.getHotScore() == null || document.getHotScore() <= 0) {
                document.setHotScore(viewCount * 1.0 + collectCount * 3.0);
            }

            return document;
        }).collect(Collectors.toList());
    }
}
