package com.unimarket.search.service.impl;

import cn.hutool.core.util.StrUtil;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimarket.common.enums.ErrandStatus;
import com.unimarket.common.enums.ReviewStatus;
import com.unimarket.common.result.PageResult;
import com.unimarket.module.errand.entity.ErrandTask;
import com.unimarket.module.errand.mapper.ErrandTaskMapper;
import com.unimarket.module.user.entity.UserInfo;
import com.unimarket.module.user.mapper.UserInfoMapper;
import com.unimarket.search.document.ErrandDocument;
import com.unimarket.search.dto.ErrandSearchRequestDTO;
import com.unimarket.search.repository.ErrandSearchRepository;
import com.unimarket.search.service.ErrandSearchService;
import com.unimarket.search.vo.ErrandSearchResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 跑腿搜索服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ErrandSearchServiceImpl implements ErrandSearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final ErrandSearchRepository errandSearchRepository;
    private final ErrandTaskMapper errandTaskMapper;
    private final UserInfoMapper userInfoMapper;

    @Override
    public PageResult<ErrandSearchResultVO> search(ErrandSearchRequestDTO request) {
        NativeQuery query = buildSearchQuery(request);
        SearchHits<ErrandDocument> searchHits = elasticsearchOperations.search(query, ErrandDocument.class);

        List<ErrandSearchResultVO> resultList = searchHits.getSearchHits().stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());

        return new PageResult<>(searchHits.getTotalHits(), resultList);
    }

    private NativeQuery buildSearchQuery(ErrandSearchRequestDTO request) {
        NativeQueryBuilder queryBuilder = NativeQuery.builder();
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();

        // 关键词搜索
        if (StrUtil.isNotBlank(request.getKeyword())) {
            boolQuery.must(Query.of(q -> q
                .multiMatch(m -> m
                    .query(request.getKeyword())
                    .fields("title^3", "title.pinyin^2", "description", "taskContent", "pickupAddress", "deliveryAddress")
                    .fuzziness("AUTO")
                )
            ));
        }

        // 任务状态过滤：默认仅展示待接单任务，避免已完成/待确认任务出现在公开接单页
        Integer taskStatus = request.getTaskStatus();
        if (taskStatus == null) {
            taskStatus = ErrandStatus.PENDING.getCode();
        }
        final Integer finalTaskStatus = taskStatus;
        boolQuery.filter(Query.of(q -> q
            .term(t -> t.field("taskStatus").value(finalTaskStatus))
        ));

        // 学校筛选
        if (StrUtil.isNotBlank(request.getSchoolCode())) {
            boolQuery.filter(Query.of(q -> q
                .term(t -> t.field("schoolCode").value(request.getSchoolCode()))
            ));
        }

        // 校区筛选
        if (StrUtil.isNotBlank(request.getCampusCode())) {
            boolQuery.filter(Query.of(q -> q
                .term(t -> t.field("campusCode").value(request.getCampusCode()))
            ));
        }

        // 赏金区间
        if (request.getMinReward() != null || request.getMaxReward() != null) {
            boolQuery.filter(Query.of(q -> q
                .range(r -> {
                    r.field("reward");
                    if (request.getMinReward() != null) {
                        r.gte(co.elastic.clients.json.JsonData.of(request.getMinReward()));
                    }
                    if (request.getMaxReward() != null) {
                        r.lte(co.elastic.clients.json.JsonData.of(request.getMaxReward()));
                    }
                    return r;
                })
            ));
        }

        queryBuilder.withQuery(Query.of(q -> q.bool(boolQuery.build())));

        // 排序
        applySorting(queryBuilder, request.getSortType(), StrUtil.isNotBlank(request.getKeyword()));

        // 分页
        queryBuilder.withPageable(PageRequest.of(
            request.getPageNum() - 1,
            request.getPageSize()
        ));

        // 高亮
        if (StrUtil.isNotBlank(request.getKeyword())) {
            queryBuilder.withHighlightQuery(new HighlightQuery(
                new Highlight(
                    HighlightParameters.builder()
                        .withPreTags("<em>")
                        .withPostTags("</em>")
                        .build(),
                    List.of(new HighlightField("title"))
                ),
                ErrandDocument.class
            ));
        }

        return queryBuilder.build();
    }

    private void applySorting(NativeQueryBuilder queryBuilder, Integer sortType, boolean hasKeyword) {
        if (sortType == null) sortType = 0;

        switch (sortType) {
            case 1: // 最新
                queryBuilder.withSort(Sort.by(
                    Sort.Direction.DESC, "createTime"
                ).and(Sort.by(Sort.Direction.DESC, "taskId")));
                break;
            case 2: // 赏金升序
                queryBuilder.withSort(Sort.by(
                    Sort.Order.asc("reward"),
                    Sort.Order.desc("createTime")
                ));
                break;
            case 3: // 赏金降序
                queryBuilder.withSort(Sort.by(
                    Sort.Order.desc("reward"),
                    Sort.Order.desc("createTime")
                ));
                break;
            default: // 综合排序
                if (hasKeyword) {
                    queryBuilder.withSort(Sort.by(
                        Sort.Order.desc("_score"),
                        Sort.Order.desc("reward"),
                        Sort.Order.desc("createTime")
                    ));
                } else {
                    queryBuilder.withSort(Sort.by(
                        Sort.Order.desc("reward"),
                        Sort.Order.desc("createTime")
                    ));
                }
        }
    }

    private ErrandSearchResultVO convertToVO(SearchHit<ErrandDocument> hit) {
        ErrandDocument doc = hit.getContent();
        ErrandSearchResultVO vo = new ErrandSearchResultVO();

        vo.setTaskId(doc.getTaskId());
        vo.setDescription(doc.getDescription());
        vo.setTaskContent(doc.getTaskContent());
        vo.setReward(doc.getReward());
        vo.setTaskStatus(doc.getTaskStatus());
        vo.setPublisherId(doc.getPublisherId());
        vo.setPublisherName(doc.getPublisherName());
        vo.setPublisherAvatar(doc.getPublisherAvatar());
        vo.setSchoolCode(doc.getSchoolCode());
        vo.setCampusCode(doc.getCampusCode());
        vo.setPickupAddress(doc.getPickupAddress());
        vo.setDeliveryAddress(doc.getDeliveryAddress());
        vo.setImage(doc.getImage());
        vo.setDeadline(doc.getDeadline());
        vo.setCreateTime(doc.getCreateTime());
        vo.setScore(hit.getScore());

        // 状态描述
        ErrandStatus status = ErrandStatus.getByCode(doc.getTaskStatus());
        if (status != null) {
            vo.setStatusText(status.getDescription());
        }

        // 高亮标题
        List<String> highlightTitle = hit.getHighlightFields().get("title");
        if (highlightTitle != null && !highlightTitle.isEmpty()) {
            vo.setTitle(highlightTitle.get(0));
        } else {
            vo.setTitle(doc.getTitle());
        }

        return vo;
    }

    @Override
    public void syncErrand(Long taskId) {
        ErrandTask task = errandTaskMapper.selectById(taskId);
        if (task == null) {
            log.warn("跑腿任务不存在，跳过同步: taskId={}", taskId);
            return;
        }
        if (!isReviewPassed(task.getReviewStatus())) {
            errandSearchRepository.deleteById(taskId);
            log.info("跑腿任务未通过审核，已从ES移除: taskId={}, reviewStatus={}", taskId, task.getReviewStatus());
            return;
        }

        ErrandDocument document = convertToDocument(task);
        errandSearchRepository.save(document);
        log.info("跑腿任务同步到ES成功: taskId={}", taskId);
    }

    @Override
    public void syncErrandBatch(List<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return;
        }
        try {
            List<ErrandTask> tasks = errandTaskMapper.selectBatchIds(taskIds);
            if (tasks.isEmpty()) {
                return;
            }

            List<Long> deleteIds = tasks.stream()
                    .filter(task -> !isReviewPassed(task.getReviewStatus()))
                    .map(ErrandTask::getTaskId)
                    .collect(Collectors.toList());
            if (!deleteIds.isEmpty()) {
                errandSearchRepository.deleteAllById(deleteIds);
            }

            List<ErrandTask> passedTasks = tasks.stream()
                    .filter(task -> isReviewPassed(task.getReviewStatus()))
                    .collect(Collectors.toList());
            if (passedTasks.isEmpty()) {
                log.info("批量同步跑腿任务完成：无审核通过任务，已删除未通过任务索引 count={}", deleteIds.size());
                return;
            }

            List<ErrandDocument> documents = convertToDocuments(passedTasks);
            errandSearchRepository.saveAll(documents);
            log.info("批量同步跑腿任务到ES成功: saveCount={}, deleteCount={}", documents.size(), deleteIds.size());
        } catch (Exception e) {
            log.error("批量同步跑腿任务到ES失败", e);
        }
    }

    @Override
    public void deleteErrand(Long taskId) {
        errandSearchRepository.deleteById(taskId);
        log.info("从ES删除跑腿任务成功: taskId={}", taskId);
    }

    @Override
    public void fullSyncErrands() {
        log.info("开始全量同步跑腿任务到ES...");
        try {
            int pageSize = 500;
            int pageNum = 1;
            long totalSynced = 0;

            while (true) {
                Page<ErrandTask> page = new Page<>(pageNum, pageSize);
                LambdaQueryWrapper<ErrandTask> wrapper = new LambdaQueryWrapper<>();
                // 仅同步审核通过且未取消任务
                wrapper.in(ErrandTask::getReviewStatus, ReviewStatus.AI_PASSED.getCode(), ReviewStatus.MANUAL_PASSED.getCode())
                       .ne(ErrandTask::getTaskStatus, ErrandStatus.CANCELLED.getCode())
                       .orderByAsc(ErrandTask::getTaskId);

                Page<ErrandTask> taskPage = errandTaskMapper.selectPage(page, wrapper);
                List<ErrandTask> tasks = taskPage.getRecords();

                if (tasks.isEmpty()) {
                    break;
                }

                List<ErrandDocument> documents = convertToDocuments(tasks);
                errandSearchRepository.saveAll(documents);

                totalSynced += documents.size();
                log.info("全量同步进度: 已同步 {}/{} 条", totalSynced, taskPage.getTotal());

                if (!taskPage.hasNext()) {
                    break;
                }
                pageNum++;
            }

            log.info("全量同步完成，共同步{}条跑腿任务", totalSynced);
        } catch (Exception e) {
            log.error("全量同步跑腿任务失败", e);
        }
    }

    @Override
    public void createOrUpdateErrandIndex() {
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(ErrandDocument.class);

            if (indexOps.exists()) {
                indexOps.delete();
                log.info("已删除旧的ES索引: errand（将使用正确的分词器设置重建）");
            }

            indexOps.create();
            indexOps.putMapping();
            log.info("ES索引创建成功: errand（已应用ik/pinyin分词器设置）");
        } catch (Exception e) {
            log.error("创建ES索引失败: errand", e);
        }
    }

    private ErrandDocument convertToDocument(ErrandTask task) {
        ErrandDocument document = new ErrandDocument();
        document.setTaskId(task.getTaskId());
        document.setTitle(task.getTitle());
        document.setDescription(task.getDescription());
        document.setTaskContent(task.getTaskContent());
        document.setReward(task.getReward());
        document.setTaskStatus(task.getTaskStatus());
        document.setPublisherId(task.getPublisherId());
        document.setSchoolCode(task.getSchoolCode());
        document.setCampusCode(task.getCampusCode());
        document.setPickupAddress(task.getPickupAddress());
        document.setDeliveryAddress(task.getDeliveryAddress());
        document.setCreateTime(task.getCreateTime());
        document.setUpdateTime(task.getUpdateTime());
        document.setDeadline(task.getDeadline());

        // 取 imageList 第一张
        if (StrUtil.isNotBlank(task.getImageList())) {
            String images = task.getImageList();
            if (images.startsWith("[")) {
                images = images.substring(1);
            }
            if (images.endsWith("]")) {
                images = images.substring(0, images.length() - 1);
            }
            String[] arr = images.split(",");
            if (arr.length > 0) {
                String first = arr[0].trim().replace("\"", "");
                if (!first.isEmpty()) {
                    document.setImage(first);
                }
            }
        }

        // 查询发布者信息
        UserInfo publisher = userInfoMapper.selectById(task.getPublisherId());
        if (publisher != null) {
            document.setPublisherName(publisher.getNickName());
            document.setPublisherAvatar(publisher.getImageUrl());
        }

        return document;
    }

    private List<ErrandDocument> convertToDocuments(List<ErrandTask> tasks) {
        if (tasks.isEmpty()) {
            return new ArrayList<>();
        }

        // 批量查询发布者信息
        List<Long> publisherIds = tasks.stream()
            .map(ErrandTask::getPublisherId)
            .distinct()
            .collect(Collectors.toList());
        Map<Long, UserInfo> publisherMap = userInfoMapper.selectBatchIds(publisherIds).stream()
            .collect(Collectors.toMap(UserInfo::getUserId, u -> u));

        return tasks.stream().map(task -> {
            ErrandDocument document = new ErrandDocument();
            document.setTaskId(task.getTaskId());
            document.setTitle(task.getTitle());
            document.setDescription(task.getDescription());
            document.setTaskContent(task.getTaskContent());
            document.setReward(task.getReward());
            document.setTaskStatus(task.getTaskStatus());
            document.setPublisherId(task.getPublisherId());
            document.setSchoolCode(task.getSchoolCode());
            document.setCampusCode(task.getCampusCode());
            document.setPickupAddress(task.getPickupAddress());
            document.setDeliveryAddress(task.getDeliveryAddress());
            document.setCreateTime(task.getCreateTime());
            document.setUpdateTime(task.getUpdateTime());
            document.setDeadline(task.getDeadline());

            // 取 imageList 第一张
            if (StrUtil.isNotBlank(task.getImageList())) {
                String images = task.getImageList();
                if (images.startsWith("[")) {
                    images = images.substring(1);
                }
                if (images.endsWith("]")) {
                    images = images.substring(0, images.length() - 1);
                }
                String[] arr = images.split(",");
                if (arr.length > 0) {
                    String first = arr[0].trim().replace("\"", "");
                    if (!first.isEmpty()) {
                        document.setImage(first);
                    }
                }
            }

            UserInfo publisher = publisherMap.get(task.getPublisherId());
            if (publisher != null) {
                document.setPublisherName(publisher.getNickName());
                document.setPublisherAvatar(publisher.getImageUrl());
            }

            return document;
        }).collect(Collectors.toList());
    }

    private boolean isReviewPassed(Integer reviewStatus) {
        return ReviewStatus.AI_PASSED.getCode().equals(reviewStatus)
                || ReviewStatus.MANUAL_PASSED.getCode().equals(reviewStatus);
    }
}

