package com.unimarket.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果封装类
 *
 * @param <T> 数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页数据
     */
    private List<T> records;

    /**
     * 当前页码
     */
    private Long pageNum;

    /**
     * 每页条数
     */
    private Long pageSize;

    /**
     * 总页数
     */
    private Long pages;

    /**
     * 构造方法（简化版）
     */
    public PageResult(Long total, List<T> records) {
        this.total = total;
        this.records = records;
    }

    /**
     * 静态工厂方法
     */
    public static <T> PageResult<T> of(List<T> records, Long total) {
        return new PageResult<>(total, records);
    }

    /**
     * 计算总页数
     */
    public void calculatePages() {
        if (this.pageSize != null && this.pageSize > 0) {
            this.pages = (this.total + this.pageSize - 1) / this.pageSize;
        }
    }
}