package org.leocoder.picture.common;

import lombok.Data;

import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07
 * @description : 分页结果封装类
 */
@Data
public class PageResult<T> {


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
    private Integer pageNum;

    /**
     * 每页记录数
     */
    private Integer pageSize;

    /**
     * 总页数
     */
    private Integer pages;

    /**
     * 是否有前一页
     */
    private Boolean hasPrevious;

    /**
     * 是否有下一页
     */
    private Boolean hasNext;

    /**
     * 创建分页结果对象
     *
     * @param total    总记录数
     * @param records  当前页数据
     * @param pageNum  当前页码
     * @param pageSize 每页记录数
     * @return 分页结果对象
     */
    public static <T> PageResult<T> build(Long total, List<T> records, Integer pageNum, Integer pageSize) {
        PageResult<T> pageResult = new PageResult<>();
        pageResult.setTotal(total);
        pageResult.setRecords(records);
        pageResult.setPageNum(pageNum);
        pageResult.setPageSize(pageSize);

        // 计算总页数
        long pages = (total + pageSize - 1) / pageSize;
        pageResult.setPages((int) pages);

        // 计算是否有前一页和下一页
        pageResult.setHasPrevious(pageNum > 1);
        pageResult.setHasNext(pageNum < pages);

        return pageResult;
    }
}