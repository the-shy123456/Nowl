package com.unimarket.search.service;

import com.unimarket.common.result.PageResult;
import com.unimarket.search.dto.SearchRequestDTO;
import com.unimarket.search.vo.SearchResultVO;

import java.util.List;

/**
 * 搜索服务接口
 */
public interface SearchService {

    /**
     * 商品搜索
     * @param request 搜索请求
     * @return 搜索结果
     */
    PageResult<SearchResultVO> search(SearchRequestDTO request, Long userId);

    /**
     * 搜索建议（自动补全）
     * @param keyword 关键词前缀
     * @param size 返回数量
     * @return 建议列表
     */
    List<String> suggest(String keyword, int size);

    /**
     * 获取热搜词
     * @param schoolCode 学校编码（可选，用于获取校内热搜）
     * @param size 返回数量
     * @return 热搜词列表
     */
    List<String> getHotWords(String schoolCode, int size);

    /**
     * 获取用户搜索历史
     * @param userId 用户ID
     * @param size 返回数量
     * @return 搜索历史列表
     */
    List<String> getSearchHistory(Long userId, int size);

    /**
     * 记录搜索历史
     * @param userId 用户ID
     * @param keyword 搜索关键词
     */
    void recordSearchHistory(Long userId, String keyword);

    /**
     * 清空用户搜索历史
     * @param userId 用户ID
     */
    void clearSearchHistory(Long userId);

    /**
     * 增加热搜词计数
     * @param keyword 搜索关键词
     * @param schoolCode 学校编码
     */
    void incrementHotWord(String keyword, String schoolCode);
}
