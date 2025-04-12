package org.leocoder.picture.domain.mapstruct;

import org.leocoder.picture.domain.pojo.User;
import org.leocoder.picture.domain.vo.user.UserVO;
import org.mapstruct.Mapper;
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
}
