package com.unimarket.search.service.impl;

import cn.hutool.core.util.StrUtil;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unimarket.common.result.PageResult;
import com.unimarket.common.utils.RedisCache;
import com.unimarket.module.goods.entity.CollectionRecord;
import com.unimarket.module.goods.mapper.CollectionRecordMapper;
import com.unimarket.module.school.entity.SchoolInfo;
import com.unimarket.module.school.mapper.SchoolInfoMapper;
import com.unimarket.module.user.entity.UserInfo;
import com.unimarket.module.user.mapper.UserInfoMapper;
import com.unimarket.search.document.GoodsDocument;
import com.unimarket.search.dto.SearchRequestDTO;
import com.unimarket.search.service.SearchService;
import com.unimarket.search.vo.SearchResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 搜索服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final ElasticsearchOperations elasticsearchOperations;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisCache redisCache;
    private final CollectionRecordMapper collectionRecordMapper;
    private final UserInfoMapper userInfoMapper;
    private final SchoolInfoMapper schoolInfoMapper;

    private static final String SEARCH_HISTORY_KEY = "search:history:";
    private static final String HOT_WORD_KEY = "search:hot:";
    private static final int MAX_HISTORY_SIZE = 10;
    private static final int MAX_QUERY_SIZE = 100;
    private static final long HOT_WORD_EXPIRE_DAYS = 7;

    @Override
    public PageResult<SearchResultVO> search(SearchRequestDTO request, Long userId) {
        normalizePageQuery(request);
        // 构建查询
        NativeQuery query = buildSearchQuery(request);

        // 执行搜索
        SearchHits<GoodsDocument> searchHits = elasticsearchOperations.search(query, GoodsDocument.class);

        // 转换结果
        List<SearchResultVO> resultList = searchHits.getSearchHits().stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());

        fillSchoolCampusInfo(resultList);
        fillCollectedStatus(resultList, userId);

        return new PageResult<>(searchHits.getTotalHits(), resultList);
    }
    /**
     * 构建搜索查询
     */
    private NativeQuery buildSearchQuery(SearchRequestDTO request) {
        NativeQueryBuilder queryBuilder = NativeQuery.builder();
        // 构建布尔查询
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();
        // 关键词搜索
        if (StrUtil.isNotBlank(request.getKeyword())) {
            // 多字段匹配：标题、标题拼音、描述、分类名
            boolQuery.must(Query.of(q -> q
                .multiMatch(m -> m
                    .query(request.getKeyword())
                    .fields("title^3", "title.pinyin^2", "description", "categoryName")
                    .fuzziness("AUTO")
                )
            ));
        }
        // 过滤条件
        // 分类筛选：支持单个分类ID或多个分类ID
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            // 多个分类ID（一级分类下的所有子分类）
            List<FieldValue> categoryValues = request.getCategoryIds().stream()
                .map(FieldValue::of)
                .collect(Collectors.toList());
            boolQuery.filter(Query.of(q -> q
                .terms(t -> t.field("categoryId").terms(v -> v.value(categoryValues)))
            ));
        } else if (request.getCategoryId() != null) {
            // 单个分类ID（二级分类精确匹配）
            boolQuery.filter(Query.of(q -> q
                .term(t -> t.field("categoryId").value(request.getCategoryId()))
            ));
        }
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
        // 价格区间
        if (request.getMinPrice() != null || request.getMaxPrice() != null) {
            boolQuery.filter(Query.of(q -> q
                .range(r -> {
                    r.field("price");
                    if (request.getMinPrice() != null) {
                        r.gte(co.elastic.clients.json.JsonData.of(request.getMinPrice()));
                    }
                    if (request.getMaxPrice() != null) {
                        r.lte(co.elastic.clients.json.JsonData.of(request.getMaxPrice()));
                    }
                    return r;
                })
            ));
        }
        // 交易状态（默认只查在售）
        Integer tradeStatus = request.getTradeStatus() != null ? request.getTradeStatus() : 0;
        boolQuery.filter(Query.of(q -> q
            .term(t -> t.field("tradeStatus").value(tradeStatus))
        ));
        // 审核状态（只查已通过）
        boolQuery.filter(Query.of(q -> q
            .terms(t -> t
                .field("reviewStatus")
                .terms(v -> v.value(Arrays.asList(
                    FieldValue.of(1),
                    FieldValue.of(2)
                )))
            )
        ));
        // 卖家筛选
        if (request.getSellerId() != null) {
            boolQuery.filter(Query.of(q -> q
                .term(t -> t.field("sellerId").value(request.getSellerId()))
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
                GoodsDocument.class
            ));
        }
        return queryBuilder.build();
    }

    /**
     * 应用排序
     */
    private void applySorting(NativeQueryBuilder queryBuilder, Integer sortType, boolean hasKeyword) {
        if (sortType == null) sortType = 0;

        switch (sortType) {
            case 1: // 最新
                queryBuilder.withSort(Sort.by(
                    Sort.Order.desc("createTime"),
                    Sort.Order.desc("productId")
                ));
                break;
            case 2: // 价格升序
                queryBuilder.withSort(Sort.by(Sort.Order.asc("price"), Sort.Order.desc("createTime")));
                break;
            case 3: // 价格降序
                queryBuilder.withSort(Sort.by(Sort.Order.desc("price"), Sort.Order.desc("createTime")));
                break;
            case 4: // 热度
                queryBuilder.withSort(Sort.by(
                    Sort.Order.desc("hotScore"),
                    Sort.Order.desc("collectCount"),
                    Sort.Order.desc("viewCount"),
                    Sort.Order.desc("createTime")
                ));
                break;
            default: // 综合排序
                if (hasKeyword) {
                    queryBuilder.withSort(Sort.by(
                        Sort.Order.desc("_score"),
                        Sort.Order.desc("hotScore"),
                        Sort.Order.desc("collectCount"),
                        Sort.Order.desc("viewCount"),
                        Sort.Order.desc("createTime")
                    ));
                } else {
                    queryBuilder.withSort(Sort.by(
                        Sort.Order.desc("hotScore"),
                        Sort.Order.desc("collectCount"),
                        Sort.Order.desc("viewCount"),
                        Sort.Order.desc("createTime")
                    ));
                }
        }
    }

    /**
     * 转换为VO
     */
    private SearchResultVO convertToVO(SearchHit<GoodsDocument> hit) {
        GoodsDocument doc = hit.getContent();
        SearchResultVO vo = new SearchResultVO();

        vo.setProductId(doc.getProductId());
        vo.setDescription(doc.getDescription());
        vo.setCategoryId(doc.getCategoryId());
        vo.setCategoryName(doc.getCategoryName());
        vo.setPrice(doc.getPrice());
        vo.setSellerId(doc.getSellerId());
        vo.setSellerName(doc.getSellerName());
        vo.setSellerAvatar(doc.getSellerAvatar());
        vo.setSchoolCode(doc.getSchoolCode());
        vo.setCampusCode(doc.getCampusCode());
        vo.setTradeStatus(doc.getTradeStatus());
        vo.setImage(doc.getImage());
        vo.setCollectCount(doc.getCollectCount());
        vo.setViewCount(doc.getViewCount());
        vo.setHotScore(doc.getHotScore());
        vo.setScore(hit.getScore());
        vo.setCreateTime(doc.getCreateTime());

        // 高亮标题
        List<String> highlightTitle = hit.getHighlightFields().get("title");
        if (highlightTitle != null && !highlightTitle.isEmpty()) {
            vo.setTitle(highlightTitle.get(0));
        } else {
            vo.setTitle(doc.getTitle());
        }

        return vo;
    }

    private void fillSchoolCampusInfo(List<SearchResultVO> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        // 只有当 ES 文档里缺少 school/campus 时，才回表查卖家信息兜底，避免每次搜索都打 MySQL。
        boolean needSellerFallback = items.stream().anyMatch(item ->
            StrUtil.isBlank(item.getSchoolCode()) || StrUtil.isBlank(item.getCampusCode())
        );
        Map<Long, UserInfo> sellerMap = new HashMap<>();
        if (needSellerFallback) {
            Set<Long> sellerIds = items.stream()
                .filter(item -> StrUtil.isBlank(item.getSchoolCode()) || StrUtil.isBlank(item.getCampusCode()))
                .map(SearchResultVO::getSellerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            if (!sellerIds.isEmpty()) {
                List<UserInfo> sellers = userInfoMapper.selectBatchIds(sellerIds);
                sellerMap = sellers.stream()
                    .collect(Collectors.toMap(UserInfo::getUserId, user -> user, (a, b) -> a));
            }
        }

        Set<String> schoolCodes = new HashSet<>();
        Set<String> campusCodes = new HashSet<>();
        for (SearchResultVO item : items) {
            UserInfo seller = sellerMap.get(item.getSellerId());
            String resolvedSchoolCode = resolveSchoolCode(item.getSchoolCode(), seller);
            String resolvedCampusCode = resolveCampusCode(item.getCampusCode(), seller);
            item.setSchoolCode(resolvedSchoolCode);
            item.setCampusCode(resolvedCampusCode);
            if (StrUtil.isNotBlank(resolvedSchoolCode) && StrUtil.isNotBlank(resolvedCampusCode)) {
                schoolCodes.add(resolvedSchoolCode);
                campusCodes.add(resolvedCampusCode);
            }
        }

        // 优先复用学校/校区缓存（SchoolServiceImpl 已缓存 campus:list:<schoolCode>），命中则无需查询 SchoolInfo 表。
        Map<String, SchoolInfo> schoolMap = new HashMap<>();
        Set<String> unresolvedKeys = new HashSet<>();
        if (!schoolCodes.isEmpty()) {
            for (String schoolCode : schoolCodes) {
                // 复用 unimarket-core 的缓存 key 规范
                List<SchoolInfo> cachedCampuses = redisCache.getCacheList("campus:list:" + schoolCode);
                if (cachedCampuses == null || cachedCampuses.isEmpty()) {
                    continue;
                }
                for (SchoolInfo campus : cachedCampuses) {
                    String key = buildSchoolCampusKey(campus.getSchoolCode(), campus.getCampusCode());
                    if (key != null) {
                        schoolMap.putIfAbsent(key, campus);
                    }
                }
            }
        }

        for (SearchResultVO item : items) {
            String key = buildSchoolCampusKey(item.getSchoolCode(), item.getCampusCode());
            if (key != null && !schoolMap.containsKey(key)) {
                unresolvedKeys.add(key);
            }
        }

        // 缓存未命中的 key 再回表查询一次兜底（只查缺失部分）
        if (!unresolvedKeys.isEmpty()) {
            Set<String> missSchoolCodes = unresolvedKeys.stream()
                .map(k -> k.split("\\|", 2)[0])
                .collect(Collectors.toSet());
            Set<String> missCampusCodes = unresolvedKeys.stream()
                .map(k -> k.split("\\|", 2)[1])
                .collect(Collectors.toSet());
            LambdaQueryWrapper<SchoolInfo> schoolWrapper = new LambdaQueryWrapper<>();
            schoolWrapper.in(SchoolInfo::getSchoolCode, missSchoolCodes)
                .in(SchoolInfo::getCampusCode, missCampusCodes)
                .eq(SchoolInfo::getStatus, 1);
            List<SchoolInfo> schools = schoolInfoMapper.selectList(schoolWrapper);
            for (SchoolInfo school : schools) {
                String key = buildSchoolCampusKey(school.getSchoolCode(), school.getCampusCode());
                if (key != null) {
                    schoolMap.putIfAbsent(key, school);
                }
            }
        }

        for (SearchResultVO item : items) {
            String schoolCode = item.getSchoolCode();
            String campusCode = item.getCampusCode();
            SchoolInfo schoolInfo = schoolMap.get(buildSchoolCampusKey(schoolCode, campusCode));
            if (schoolInfo != null) {
                item.setSchoolName(schoolInfo.getSchoolName());
                item.setCampusName(schoolInfo.getCampusName());
            } else {
                item.setSchoolName(schoolCode);
                item.setCampusName(campusCode);
            }
        }
    }

    private String resolveSchoolCode(String schoolCode, UserInfo seller) {
        if (StrUtil.isNotBlank(schoolCode)) {
            return schoolCode;
        }
        return seller == null ? null : seller.getSchoolCode();
    }

    private String resolveCampusCode(String campusCode, UserInfo seller) {
        if (StrUtil.isNotBlank(campusCode)) {
            return campusCode;
        }
        return seller == null ? null : seller.getCampusCode();
    }

    private String buildSchoolCampusKey(String schoolCode, String campusCode) {
        if (StrUtil.isBlank(schoolCode) || StrUtil.isBlank(campusCode)) {
            return null;
        }
        return schoolCode + "|" + campusCode;
    }

    private void fillCollectedStatus(List<SearchResultVO> items, Long userId) {
        if (userId == null || items == null || items.isEmpty()) {
            return;
        }
        List<Long> productIds = items.stream()
            .map(SearchResultVO::getProductId)
            .distinct()
            .collect(Collectors.toList());
        if (productIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<CollectionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CollectionRecord::getUserId, userId)
            .in(CollectionRecord::getProductId, productIds);
        List<CollectionRecord> records = collectionRecordMapper.selectList(wrapper);
        if (records.isEmpty()) {
            return;
        }
        Set<Long> collectedIds = records.stream()
            .map(CollectionRecord::getProductId)
            .collect(Collectors.toSet());
        for (SearchResultVO item : items) {
            item.setIsCollected(collectedIds.contains(item.getProductId()));
        }
    }

    @Override
    public List<String> suggest(String keyword, int size) {
        if (StrUtil.isBlank(keyword)) {
            return Collections.emptyList();
        }
        int safeSize = clamp(size, 1, MAX_QUERY_SIZE);

        // 使用前缀查询实现简单的搜索建议
        NativeQuery query = NativeQuery.builder()
            .withQuery(Query.of(q -> q
                .prefix(p -> p
                    .field("title.keyword")
                    .value(keyword)
                )
            ))
            .withPageable(PageRequest.of(0, safeSize))
            .build();

        SearchHits<GoodsDocument> hits = elasticsearchOperations.search(query, GoodsDocument.class);

        return hits.getSearchHits().stream()
            .map(hit -> hit.getContent().getTitle())
            .distinct()
            .limit(safeSize)
            .collect(Collectors.toList());
    }

    @Override
    public List<String> getHotWords(String schoolCode, int size) {
        int safeSize = clamp(size, 1, MAX_QUERY_SIZE);
        String key = StrUtil.isNotBlank(schoolCode)
            ? HOT_WORD_KEY + schoolCode
            : HOT_WORD_KEY + "all";

        Set<String> hotWords = stringRedisTemplate.opsForZSet()
            .reverseRange(key, 0, safeSize - 1);

        return hotWords != null ? new ArrayList<>(hotWords) : Collections.emptyList();
    }

    @Override
    public List<String> getSearchHistory(Long userId, int size) {
        int safeSize = clamp(size, 1, MAX_QUERY_SIZE);
        String key = SEARCH_HISTORY_KEY + userId;
        List<String> history = stringRedisTemplate.opsForList().range(key, 0, safeSize - 1);
        return history != null ? history : Collections.emptyList();
    }

    @Override
    public void recordSearchHistory(Long userId, String keyword) {
        if (userId == null || StrUtil.isBlank(keyword)) {
            return;
        }

        String key = SEARCH_HISTORY_KEY + userId;

        // 移除已存在的相同关键词
        stringRedisTemplate.opsForList().remove(key, 0, keyword);

        // 添加到列表头部
        stringRedisTemplate.opsForList().leftPush(key, keyword);

        // 保持列表长度
        stringRedisTemplate.opsForList().trim(key, 0, MAX_HISTORY_SIZE - 1);

        // 设置过期时间（30天）
        stringRedisTemplate.expire(key, 30, TimeUnit.DAYS);
    }

    @Override
    public void clearSearchHistory(Long userId) {
        String key = SEARCH_HISTORY_KEY + userId;
        stringRedisTemplate.delete(key);
    }

    @Override
    public void incrementHotWord(String keyword, String schoolCode) {
        if (StrUtil.isBlank(keyword)) {
            return;
        }

        // 全局热搜
        String globalKey = HOT_WORD_KEY + "all";
        stringRedisTemplate.opsForZSet().incrementScore(globalKey, keyword, 1);
        stringRedisTemplate.expire(globalKey, HOT_WORD_EXPIRE_DAYS, TimeUnit.DAYS);

        // 学校热搜
        if (StrUtil.isNotBlank(schoolCode)) {
            String schoolKey = HOT_WORD_KEY + schoolCode;
            stringRedisTemplate.opsForZSet().incrementScore(schoolKey, keyword, 1);
            stringRedisTemplate.expire(schoolKey, HOT_WORD_EXPIRE_DAYS, TimeUnit.DAYS);
        }
    }

    private void normalizePageQuery(SearchRequestDTO request) {
        if (request == null) {
            return;
        }
        int pageNum = request.getPageNum() == null ? 1 : request.getPageNum();
        int pageSize = request.getPageSize() == null ? 10 : request.getPageSize();
        request.setPageNum(clamp(pageNum, 1, Integer.MAX_VALUE));
        request.setPageSize(clamp(pageSize, 1, MAX_QUERY_SIZE));
    }

    private int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }
}
