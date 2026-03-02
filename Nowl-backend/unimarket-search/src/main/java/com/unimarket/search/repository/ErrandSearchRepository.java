package com.unimarket.search.repository;

import com.unimarket.search.document.ErrandDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 跑腿任务ES仓库
 */
@Repository
public interface ErrandSearchRepository extends ElasticsearchRepository<ErrandDocument, Long> {

    List<ErrandDocument> findBySchoolCode(String schoolCode);

    List<ErrandDocument> findByTaskStatus(Integer taskStatus);
}
