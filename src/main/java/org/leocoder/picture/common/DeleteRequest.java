package org.leocoder.picture.common;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07 13:35
 * @description : 删除请求对象
 */
@Data
@ApiModel(description = "通用删除请求参数")
public class DeleteRequest implements Serializable {

    /**
     * id
     */
    @ApiModelProperty(value = "ID", required = true)
    private Long id;

    private static final long serialVersionUID = 1L;
}
