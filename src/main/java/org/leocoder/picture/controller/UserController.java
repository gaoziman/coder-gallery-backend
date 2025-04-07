package org.leocoder.picture.controller;

import cn.hutool.core.util.ObjectUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.leocoder.picture.common.Result;
import org.leocoder.picture.common.ResultUtils;
import org.leocoder.picture.domain.dto.user.UserLoginRequest;
import org.leocoder.picture.domain.dto.user.UserRegisterRequest;
import org.leocoder.picture.domain.dto.user.UserUpdatePasswordRequest;
import org.leocoder.picture.domain.dto.user.UserUpdateRequest;
import org.leocoder.picture.domain.vo.user.LoginUserVO;
import org.leocoder.picture.domain.vo.user.UserVO;
import org.leocoder.picture.exception.ErrorCode;
import org.leocoder.picture.exception.ThrowUtils;
import org.leocoder.picture.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07 13:35
 * @description : 用户相关接口
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Api(tags = "登录管理")
public class UserController {


    private final UserService userService;


    @ApiOperation(value = "用户注册")
    @PostMapping("/register")
    public Result<Long> userRegister(@RequestBody UserRegisterRequest requestParam) {
        ThrowUtils.throwIf(ObjectUtil.isNull(requestParam), ErrorCode.PARAMS_ERROR);
        String userAccount = requestParam.getAccount();
        String userPassword = requestParam.getPassword();
        String checkPassword = requestParam.getCheckPassword();
        Long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }


    @ApiOperation(value = "用户登录")
    @PostMapping("/login")
    public Result<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjectUtil.isNull(userLoginRequest), ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getAccount();
        String userPassword = userLoginRequest.getPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword);
        return ResultUtils.success(loginUserVO);
    }


    @ApiOperation(value = "获取当前登录用户信息")
    @GetMapping("/current")
    public Result<UserVO> getCurrentUser() {
        UserVO userVO = userService.getCurrentUser();
        return ResultUtils.success(userVO);
    }

    @ApiOperation(value = "用户注销")
    @PostMapping("/logout")
    public Result<Boolean> userLogout() {
        boolean result = userService.userLogout();
        return ResultUtils.success(result);
    }

    @ApiOperation(value = "修改用户信息")
    @PutMapping("/update")
    public Result<Boolean> updateUserInfo(@RequestBody UserUpdateRequest requestParam) {
        ThrowUtils.throwIf(ObjectUtil.isNull(requestParam), ErrorCode.PARAMS_ERROR);
        boolean result = userService.updateUserInfo(requestParam);
        return ResultUtils.success(result);
    }

    @ApiOperation(value = "修改用户密码")
    @PutMapping("/update/password")
    public Result<Boolean> updateUserPassword(@RequestBody UserUpdatePasswordRequest requestParam) {
        ThrowUtils.throwIf(ObjectUtil.isNull(requestParam), ErrorCode.PARAMS_ERROR);
        boolean result = userService.updateUserPassword(
                requestParam.getOldPassword(),
                requestParam.getNewPassword(),
                requestParam.getCheckPassword()
        );
        return ResultUtils.success(result);
    }
}
