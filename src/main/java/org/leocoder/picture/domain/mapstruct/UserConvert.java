package org.leocoder.picture.domain.mapstruct;

import cn.dev33.satoken.stp.SaTokenInfo;
import org.leocoder.picture.domain.pojo.User;
import org.leocoder.picture.domain.vo.user.LoginUserVO;
import org.leocoder.picture.domain.vo.user.UserVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-13 01:32
 * @description :
 */
@Mapper
public interface UserConvert {
    UserConvert INSTANCE = Mappers.getMapper(UserConvert.class);

    UserVO toUserVO(User user);

    List<UserVO> toUserVOList(List<User> userList);


    /**
     * 将User对象和SaTokenInfo转换为LoginUserVO
     *
     * @param user      用户对象
     * @param tokenInfo 令牌信息
     * @return 登录用户视图对象
     */
    @Mapping(source = "user.phone", target = "userPhone")
    @Mapping(source = "tokenInfo.tokenName", target = "tokenName")
    @Mapping(source = "tokenInfo.tokenValue", target = "tokenValue")
    @Mapping(source = "tokenInfo.tokenTimeout", target = "tokenTimeout")
    LoginUserVO toLoginUserVO(User user, SaTokenInfo tokenInfo);
}
