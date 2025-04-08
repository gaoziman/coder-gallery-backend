package org.leocoder.picture.domain.dto.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07
 * @description : 批量删除请求
 */
@Data
@ApiModel(value = "BatchDeleteRequest", description = "批量删除请求参数")
public class BatchDeleteRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "ID列表", required = true)
    private List<Long> ids;
}