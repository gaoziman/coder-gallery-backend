package org.leocoder.picture.domain.mapstruct;

import org.leocoder.picture.domain.pojo.OperationLog;
import org.leocoder.picture.domain.vo.log.OperationLogVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-13
 * @description : 操作日志对象转换接口
 */
@Mapper
public interface OperationLogConvert {
    OperationLogConvert INSTANCE = Mappers.getMapper(OperationLogConvert.class);

    /**
     * 将 OperationLog 实体转换为 OperationLogVO
     * 注意：用户相关信息需要在服务层单独设置
     * @param operationLog 操作日志实体
     * @return 操作日志VO
     */
    OperationLogVO toOperationLogVO(OperationLog operationLog);

    /**
     * 将 OperationLog 实体列表转换为 OperationLogVO 列表
     * @param operationLogList 操作日志实体列表
     * @return 操作日志VO列表
     */
    List<OperationLogVO> toOperationLogVOList(List<OperationLog> operationLogList);
}