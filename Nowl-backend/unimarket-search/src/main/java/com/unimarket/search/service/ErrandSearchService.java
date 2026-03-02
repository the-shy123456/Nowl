package com.unimarket.search.service;

import com.unimarket.common.result.PageResult;
import com.unimarket.search.dto.ErrandSearchRequestDTO;
import com.unimarket.search.vo.ErrandSearchResultVO;

import java.util.List;

/**
 * 跑腿搜索服务接口
 */
public interface ErrandSearchService {

    PageResult<ErrandSearchResultVO> search(ErrandSearchRequestDTO request);

    void syncErrand(Long taskId);

    void syncErrandBatch(List<Long> taskIds);

    void deleteErrand(Long taskId);

    void fullSyncErrands();

    void createOrUpdateErrandIndex();
}
