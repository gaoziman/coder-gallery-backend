package org.leocoder.picture.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.leocoder.picture.common.DeleteRequest;
import org.leocoder.picture.common.PageResult;
import org.leocoder.picture.common.Result;
import org.leocoder.picture.common.ResultUtils;
import org.leocoder.picture.domain.dto.user.*;
import org.leocoder.picture.domain.vo.user.UserStatisticsVO;
import org.leocoder.picture.domain.vo.user.UserVO;
import org.leocoder.picture.exception.BusinessException;
import org.leocoder.picture.exception.ErrorCode;
import org.leocoder.picture.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.regex.Pattern;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07 20:32
 * @description : 用户管理相关接口
 */
@RestController
@RequestMapping("/admin/user")
@RequiredArgsConstructor
@Api(tags = "用户管理")
public class AdminUserController {

    private final UserService userService;

    // 账号正则：4-16位，字母开头，允许字母、数字、下划线
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("^[a-zA-Z]\\w{3,15}$");
    // 密码正则：6-16位，必须包含字母和数字
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,16}$");
    // 手机号正则：1开头的11位数字
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(1[3-9])\\d{9}$");

    @PostMapping("/add")
    @ApiOperation(value = "创建用户", notes = "创建一个新用户，仅管理员可用")
    public Result<Long> addUser(@RequestBody AdminUserAddRequest requestParam) {
        if (ObjectUtil.isEmpty(requestParam)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        // 校验账号
        String account = requestParam.getAccount();
        if (StrUtil.isBlank(account)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能为空");
        }
        if (!ACCOUNT_PATTERN.matcher(account).matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号格式不正确，应为4-16位，字母开头，允许字母、数字、下划线");
        }

        // 校验密码
        String password = requestParam.getPassword();
        if (StrUtil.isBlank(password)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能为空");
        }
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码格式不正确，应为6-16位，必须包含字母和数字");
        }

        // 校验用户名（可选）
        String username = requestParam.getUsername();
        if (StrUtil.isNotBlank(username) && username.length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名长度不能超过50个字符");
        }

        // 校验手机号（可选）
        String phone = requestParam.getPhone();
        if (StrUtil.isNotBlank(phone) && !PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号格式不正确");
        }

        // 校验用户简介（可选）
        String userProfile = requestParam.getUserProfile();
        if (StrUtil.isNotBlank(userProfile) && userProfile.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户简介长度不能超过512个字符");
        }

        // 校验头像URL（可选）
        String avatar = requestParam.getAvatar();
        if (StrUtil.isNotBlank(avatar) && avatar.length() > 1024) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "头像URL长度不能超过1024个字符");
        }

        Long userId = userService.addUser(requestParam);
        return ResultUtils.success(userId);
    }

    @GetMapping("/get")
    @ApiOperation(value = "根据ID获取用户详情", notes = "获取用户完整信息，仅管理员可用")
    public Result<UserVO> getUserById(@RequestParam("id") Long id) {
        // 参数校验
        if (ObjectUtil.isEmpty(id)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID必须大于0");
        }
        UserVO userVO = userService.getUserById(id);
        return ResultUtils.success(userVO);
    }

    @GetMapping("/get/vo")
    @ApiOperation(value = "根据ID获取脱敏用户信息", notes = "获取脱敏后的用户信息")
    public Result<UserVO> getUserVOById(@RequestParam("id") Long id) {
        // 参数校验
        if (ObjectUtil.isEmpty(id)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID必须大于0");
        }
        UserVO userVO = userService.getUserVOById(id);
        return ResultUtils.success(userVO);
    }

    @DeleteMapping("/delete")
    @ApiOperation(value = "删除用户", notes = "根据ID删除用户，仅管理员可用")
    public Result<Boolean> deleteUser(DeleteRequest requestParam) {
        if (ObjectUtil.isEmpty(requestParam)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }
        Long id = requestParam.getId();
        // 参数校验
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID必须大于0");
        }
        Boolean result = userService.deleteUser(id);
        return ResultUtils.success(result);
    }

    @PutMapping("/update")
    @ApiOperation(value = "更新用户信息", notes = "管理员更新用户信息，仅管理员可用")
    public Result<Boolean> updateUser(@RequestBody AdminUserUpdateRequest requestParam) {
        // 参数校验
        if (ObjectUtil.isEmpty(requestParam)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        if (ObjectUtil.isEmpty(requestParam.getId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }

        // 校验用户名（可选）
        String username = requestParam.getUsername();
        if (StrUtil.isNotBlank(username) && username.length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名长度不能超过50个字符");
        }

        // 校验手机号（可选）
        String phone = requestParam.getPhone();
        if (StrUtil.isNotBlank(phone) && !PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号格式不正确");
        }


        // 校验头像URL（可选）
        String avatar = requestParam.getAvatar();
        if (StrUtil.isNotBlank(avatar) && avatar.length() > 1024) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "头像URL长度不能超过1024个字符");
        }

        Boolean result = userService.updateUserByAdmin(requestParam);
        return ResultUtils.success(result);
    }

    @GetMapping("/list/page")
    @ApiOperation(value = "分页获取用户列表", notes = "分页获取用户列表，支持条件查询，仅管理员可用")
    public Result<PageResult<UserVO>> listUserByPage(AdminUserQueryRequest userQueryRequest) {
        // 参数校验
        if (ObjectUtil.isEmpty(userQueryRequest)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        // 校验分页参数
        Integer pageNum = userQueryRequest.getPageNum();
        Integer pageSize = userQueryRequest.getPageSize();
        if (ObjectUtil.isNotEmpty(pageNum) && pageNum <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "页码必须大于0");
        }
        if (ObjectUtil.isNotEmpty(pageSize) && (pageSize <= 0 || pageSize > 100)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "每页条数必须在1-100之间");
        }

        PageResult<UserVO> pageResult = userService.listUserByPage(userQueryRequest);
        return ResultUtils.success(pageResult);
    }

    @PostMapping("/ban")
    @ApiOperation(value = "禁用用户", notes = "禁用指定用户，仅管理员可用")
    public Result<Boolean> banUser(@RequestParam("id") Long id) {
        // 参数校验
        if (ObjectUtil.isEmpty(id)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID必须大于0");
        }
        Boolean result = userService.banUser(id);
        return ResultUtils.success(result);
    }

    @PostMapping("/unban")
    @ApiOperation(value = "解禁用户", notes = "解除用户禁用状态，仅管理员可用")
    public Result<Boolean> unbanUser(@RequestParam("id") Long id) {
        // 参数校验
        if (ObjectUtil.isEmpty(id)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID必须大于0");
        }
        Boolean result = userService.unbanUser(id);
        return ResultUtils.success(result);
    }

    @GetMapping("/statistics")
    @ApiOperation(value = "获取用户统计信息", notes = "统计注册用户总数、本月新增用户、VIP用户数以及活跃用户比例")
    public Result<UserStatisticsVO> getUserStatistics() {
        UserStatisticsVO statistics = userService.getUserStatistics();
        return ResultUtils.success(statistics);
    }

    @PostMapping("/reset/password")
    @ApiOperation(value = "重置用户密码", notes = "重置指定用户的密码，仅管理员可用")
    public Result<Boolean> resetUserPassword(@RequestBody AdminResetPasswordRequest requestParam) {
        // 参数校验
        if (ObjectUtil.isEmpty(requestParam)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        Long userId = requestParam.getUserId();
        String newPassword = requestParam.getNewPassword();
        String checkPassword = requestParam.getCheckPassword();

        if (ObjectUtil.isEmpty(userId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }

        if (userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID必须大于0");
        }

        if (StrUtil.hasBlank(newPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能为空");
        }

        Boolean result = userService.resetUserPassword(userId, newPassword, checkPassword);
        return ResultUtils.success(result);
    }

    @DeleteMapping("/batch/delete")
    @ApiOperation(value = "批量删除用户", notes = "根据ID列表批量删除用户，仅管理员可用")
    public Result<Boolean> batchDeleteUsers(@RequestBody BatchDeleteRequest requestParam) {
        // 参数校验
        if (ObjectUtil.isEmpty(requestParam) || ObjectUtil.isEmpty(requestParam.getIds())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID列表不能为空");
        }

        // 检查ID列表是否合法
        List<Long> ids = requestParam.getIds();
        for (Long id : ids) {
            if (id <= 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID必须大于0");
            }
        }

        Boolean result = userService.batchDeleteUsers(ids);
        return ResultUtils.success(result);
    }
}