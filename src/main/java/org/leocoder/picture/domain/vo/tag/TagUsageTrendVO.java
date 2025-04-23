package org.leocoder.picture.domain.vo.tag;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-09 10:57
 * @description : 标签使用趋势数据视图对象
 */
@Data
@ApiModel(description = "标签使用趋势数据视图对象")
public class TagUsageTrendVO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @ApiModelProperty(value = "日期", example = "2025-04-01")
    private String date;
    
    @ApiModelProperty(value = "使用次数", example = "5")
    private Integer count;
}