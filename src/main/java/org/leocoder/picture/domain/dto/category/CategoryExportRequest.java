package org.leocoder.picture.domain.dto.category;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-08 14:32
 * @description : 分类导出请求
 */
@Data
@ApiModel(value = "CategoryExportRequest", description = "分类导出请求")
public class CategoryExportRequest {
    
    @ApiModelProperty(value = "是否导出所有分类")
    private Boolean exportAll;
    
    @ApiModelProperty(value = "要导出的分类ID列表")
    private List<Long> ids;
    
    @ApiModelProperty(value = "导出格式，支持xlsx, csv等")
    private String format;
}