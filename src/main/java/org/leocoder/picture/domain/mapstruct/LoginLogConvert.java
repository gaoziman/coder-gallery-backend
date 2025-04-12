package org.leocoder.picture.domain.mapstruct;

import org.leocoder.picture.domain.pojo.LoginLog;
import org.leocoder.picture.domain.vo.log.LoginLogVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-13
 * @description : 登录日志对象转换接口
 */
@Mapper
public interface LoginLogConvert {
    LoginLogConvert INSTANCE = Mappers.getMapper(LoginLogConvert.class);

    /**
     * 将 LoginLog 实体转换为 LoginLogVO
     * 注意：用户相关信息需要在服务层单独设置
     * @param loginLog 登录日志实体
     * @return 登录日志VO
     */
    LoginLogVO toLoginLogVO(LoginLog loginLog);

    /**
     * 将 LoginLog 实体列表转换为 LoginLogVO 列表
     * @param loginLogList 登录日志实体列表
     * @return 登录日志VO列表
     */
    List<LoginLogVO> toLoginLogVOList(List<LoginLog> loginLogList);
}