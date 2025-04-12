package org.leocoder.picture.common;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07
 * @description : 通用分页查询工具类
 */

public class PageUtils {

    /**
     * 默认页码
     */
    private static final int DEFAULT_PAGE_NUM = 1;
    /**
     * 默认每页记录数
     */
    private static final int DEFAULT_PAGE_SIZE = 10;
    /**
     * 最大每页记录数
     */
    private static final int MAX_PAGE_SIZE = 50;

    /**
     * 执行分页查询
     *
     * @param pageRequest   分页请求参数
     * @param queryExecutor 查询执行器
     * @param converter     结果转换器
     * @param <T>           查询结果类型
     * @param <R>           转换后结果类型
     * @return 分页结果
     */
    public static <T, R> PageResult<R> doPage(PageRequest pageRequest, 
                                             Supplier<List<T>> queryExecutor,
                                             Function<T, R> converter) {
        // 参数校验与默认值处理
        int pageNum = getPageNum(pageRequest);
        int pageSize = getPageSize(pageRequest);
        
        // 设置分页参数并执行查询
        Page<T> page = PageHelper.startPage(pageNum, pageSize)
                .doSelectPage(queryExecutor::get);
        
        // 结果处理
        if (page.getTotal() == 0) {
            return PageResult.build(0L, Collections.emptyList(), pageNum, pageSize);
        }
        
        // 转换结果
        List<R> resultList = page.getResult().stream()
                .map(converter)
                .collect(Collectors.toList());
        
        return PageResult.build(page.getTotal(), resultList, pageNum, pageSize);
    }
    
    /**
     * 获取有效的页码
     */
    private static int getPageNum(PageRequest pageRequest) {
        Integer pageNum = pageRequest != null ? pageRequest.getPageNum() : null;
        return (pageNum == null || pageNum < 1) ? DEFAULT_PAGE_NUM : pageNum;
    }
    
    /**
     * 获取有效的每页记录数
     */
    private static int getPageSize(PageRequest pageRequest) {
        Integer pageSize = pageRequest != null ? pageRequest.getPageSize() : null;
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}