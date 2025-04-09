package org.leocoder.picture.common;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-08
 * @description : 分页请求基类
 */
@Data
public class PageRequest {
    
    @ApiModelProperty(value = "页码", example = "1")
    private Integer pageNum = 1;
    
    @ApiModelProperty(value = "每页大小", example = "10")
    private Integer pageSize = 10;
}