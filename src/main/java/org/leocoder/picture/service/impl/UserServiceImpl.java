package org.leocoder.picture.service.impl;

import cn.dev33.satoken.secure.SaSecureUtil;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.leocoder.picture.common.PageResult;
import org.leocoder.picture.config.BloomFilterConfig;
import org.leocoder.picture.domain.dto.user.AdminUserAddRequest;
import org.leocoder.picture.domain.dto.user.AdminUserQueryRequest;
import org.leocoder.picture.domain.dto.user.AdminUserUpdateRequest;
import org.leocoder.picture.domain.dto.user.UserUpdateRequest;
import org.leocoder.picture.domain.pojo.User;
import org.leocoder.picture.domain.vo.user.LoginUserVO;
import org.leocoder.picture.domain.vo.user.UserStatisticsVO;
import org.leocoder.picture.domain.vo.user.UserVO;
import org.leocoder.picture.enums.UserRoleEnum;
import org.leocoder.picture.enums.UserStatusEnum;
import org.leocoder.picture.exception.BusinessException;
import org.leocoder.picture.exception.ErrorCode;
import org.leocoder.picture.mapper.UserMapper;
import org.leocoder.picture.service.UserService;
import org.leocoder.picture.utils.IpUtils;
import org.leocoder.picture.utils.SnowflakeIdGenerator;
import org.leocoder.picture.utils.UserContext;
import org.leocoder.picture.utils.UsernameGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07 13:26
 * @description : 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    private final BloomFilterConfig bloomFilterConfig;

    private final SnowflakeIdGenerator snowflakeIdGenerator;

    // 账号正则：4-16位，字母开头，允许字母、数字、下划线
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("^[a-zA-Z]\\w{3,15}$");
    // 密码正则：6-16位，必须包含字母和数字
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,16}$");


    /**
     * 用户注册
     *
     * @param userAccount   用户账号
     * @param userPassword  用户密码
     * @param checkPassword 确认密码
     * @return 注册成功的用户ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 参数校验
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        // 2. 账号格式校验
        if (!ACCOUNT_PATTERN.matcher(userAccount).matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号格式不正确，应为4-16位，字母开头，允许字母、数字、下划线");
        }

        // 3. 密码格式校验
        if (!PASSWORD_PATTERN.matcher(userPassword).matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码格式不正确，应为6-16位，必须包含字母和数字");
        }

        // 4. 两次密码是否一致
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        // 5. 布隆过滤器预判是否已存在相同账号(减少数据库查询)
        if (bloomFilterConfig.mightContain(userAccount)) {
            // 布隆过滤器判断可能存在，进一步精确查询数据库
            User existUser = userMapper.selectByAccount(userAccount);
            if (ObjectUtil.isNotNull(existUser)) {
                throw new BusinessException(ErrorCode.ACCOUNT_EXIST, "账号已存在");
            }
        }

        // 6. 密码加密
        String salt = IdUtil.simpleUUID().substring(0, 8);
        String encryptPassword = SaSecureUtil.md5BySalt(userPassword, salt);

        // 7. 构建用户对象，使用雪花算法生成ID
        LocalDateTime now = LocalDateTime.now();
        // 使用工具类生成用户名
        String generatedUsername = UsernameGenerator.generateGalleryUsername();

        // 生成雪花算法ID
        Long userId = snowflakeIdGenerator.nextId();

        User user = User.builder()
                // 设置雪花算法生成的ID
                .id(userId)
                .account(userAccount)
                .username(generatedUsername)
                .password(encryptPassword)
                .salt(salt)
                // 默认角色为普通用户
                .role(UserRoleEnum.USER.getValue())
                // 默认状态为已激活
                .status(UserStatusEnum.ACTIVE.getValue())
                .registerTime(now)
                .createTime(now)
                .updateTime(now)
                .isDeleted(0)
                .build();

        // 8. 插入数据库，注意这里需要修改Mapper方法，以支持指定ID插入
        int insertResult = userMapper.insertWithId(user);
        if (insertResult != 1) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，请稍后重试");
        }

        // 9. 将新账号添加到布隆过滤器
        bloomFilterConfig.addAccount(userAccount);

        return user.getId();
    }


    /**
     * 用户登录
     *
     * @param userAccount  用户账号
     * @param userPassword 用户密码
     * @return 登录用户信息
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword) {
        // 1. 参数校验
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        // 2. 布隆过滤器预判账号是否存在
        if (!bloomFilterConfig.mightContain(userAccount)) {
            // 布隆过滤器判断一定不存在，直接返回错误
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND, "账号不存在");
        }

        // 3. 查询用户信息
        User user = userMapper.selectByAccount(userAccount);
        if (ObjectUtil.isNull(user)) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND, "账号不存在");
        }

        // 4. 状态校验
        if (!"active".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_BANNED, "账号状态异常，请联系管理员");
        }

        // 5. 密码校验
        String encryptPassword = SaSecureUtil.md5BySalt(userPassword, user.getSalt());
        if (!encryptPassword.equals(user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        // 6. 获取登录IP - 使用IP工具类
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String ipAddress = IpUtils.getIpAddress(request);

        // 7. 更新登录信息
        LocalDateTime now = LocalDateTime.now();


        userMapper.updateLoginInfo(user.getId(), now, ipAddress);

        // 8. Sa-Token登录并获取token信息
        StpUtil.login(user.getId());
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();

        // 更新user对象的最后登录信息
        user.setLastLoginTime(now);
        user.setLastLoginIp(ipAddress);

        // 9. 将用户信息设置到当前线程上下文
        UserContext.setUser(user);

        // 10. 构建并返回登录用户VO
        return LoginUserVO.builder()
                .id(user.getId())
                .account(user.getAccount())
                .username(user.getUsername())
                .userPhone(user.getPhone())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .userProfile(user.getUserProfile())
                .tokenName(tokenInfo.getTokenName())
                .tokenValue(tokenInfo.getTokenValue())
                .tokenTimeout(tokenInfo.getTokenTimeout())
                .lastLoginTime(now)
                .lastLoginIp(ipAddress)
                .createTime(user.getCreateTime())
                .updateTime(user.getUpdateTime())
                .build();
    }


    /**
     * 获取当前登录用户信息
     *
     * @return 当前登录用户信息
     */
    @Override
    public UserVO getCurrentUser() {
        // 从线程上下文中获取当前用户
        User user = UserContext.getUser();
        if (ObjectUtil.isNull(user)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "用户未登录");
        }

        // 转换为VO对象
        return UserVO.builder()
                .id(user.getId())
                .account(user.getAccount())
                .username(user.getUsername())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .userProfile(user.getUserProfile())
                .role(user.getRole())
                .status(user.getStatus())
                .lastLoginTime(user.getLastLoginTime())
                .registerTime(user.getRegisterTime())
                .build();
    }


    /**
     * 根据账号获取用户信息
     *
     * @param account 账号
     * @return 用户信息
     */
    @Override
    public User getUserByAccount(String account) {
        if (StrUtil.isBlank(account)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号不能为空");
        }
        return userMapper.selectByAccount(account);
    }

    /**
     * 用户注销
     *
     * @return 是否成功
     */
    @Override
    public boolean userLogout() {
        // 1. 检查用户是否已登录
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "用户未登录");
        }
        try {
            // 2. 执行Sa-Token的注销操作
            StpUtil.logout();

            // 3. 清除线程上下文中的用户信息
            UserContext.clear();

            return true;
        } catch (Exception e) {
            log.error("用户注销失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注销失败，请稍后重试");
        }
    }

    /**
     * 更新用户信息
     *
     * @param userUpdateRequest 用户信息更新请求
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUserInfo(UserUpdateRequest userUpdateRequest) {
        // 1. 检查用户是否已登录
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "用户未登录");
        }

        // 2. 获取当前登录用户ID
        Long userId = StpUtil.getLoginIdAsLong();

        // 3. 校验请求参数
        if (ObjectUtil.isNull(userUpdateRequest)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        // 4. 从数据库获取最新用户信息
        User user = userMapper.selectById(userId);
        if (ObjectUtil.isNull(user)) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND, "用户不存在");
        }

        // 5. 构建更新对象
        User updateUser = new User();
        updateUser.setId(userId);

        // 只更新非空字段
        if (StrUtil.isNotBlank(userUpdateRequest.getUsername())) {
            updateUser.setUsername(userUpdateRequest.getUsername());
        }

        if (StrUtil.isNotBlank(userUpdateRequest.getPhone())) {
            // 可以添加手机号格式校验
            updateUser.setPhone(userUpdateRequest.getPhone());
        }

        if (StrUtil.isNotBlank(userUpdateRequest.getAvatar())) {
            updateUser.setAvatar(userUpdateRequest.getAvatar());
        }

        if (StrUtil.isNotBlank(userUpdateRequest.getUserProfile())) {
            updateUser.setUserProfile(userUpdateRequest.getUserProfile());
        }

        // 设置更新时间
        updateUser.setUpdateTime(LocalDateTime.now());

        // 6. 执行更新操作
        int result = userMapper.updateById(updateUser);
        if (result != 1) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新失败，请稍后重试");
        }

        // 7. 更新线程上下文中的用户信息
        User contextUser = UserContext.getUser();
        if (ObjectUtil.isNotNull(contextUser)) {
            if (StrUtil.isNotBlank(userUpdateRequest.getUsername())) {
                contextUser.setUsername(userUpdateRequest.getUsername());
            }
            if (StrUtil.isNotBlank(userUpdateRequest.getPhone())) {
                contextUser.setPhone(userUpdateRequest.getPhone());
            }
            if (StrUtil.isNotBlank(userUpdateRequest.getAvatar())) {
                contextUser.setAvatar(userUpdateRequest.getAvatar());
            }
            if (StrUtil.isNotBlank(userUpdateRequest.getUserProfile())) {
                contextUser.setUserProfile(userUpdateRequest.getUserProfile());
            }
            contextUser.setUpdateTime(LocalDateTime.now());
            UserContext.setUser(contextUser);
        }

        return true;
    }

    /**
     * 更新用户密码
     *
     * @param oldPassword   旧密码
     * @param newPassword   新密码
     * @param checkPassword 确认密码
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUserPassword(String oldPassword, String newPassword, String checkPassword) {
        // 1. 检查用户是否已登录
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ErrorCode.NOT_LOGIN, "用户未登录");
        }

        // 2. 参数校验
        if (StrUtil.hasBlank(oldPassword, newPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        // 3. 检查新密码的两次输入是否一致
        if (!newPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的新密码不一致");
        }

        //  检查新密码不能与旧密码相同
        if (newPassword.equals(oldPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码不能与旧密码相同");
        }

        // 4. 校验新密码格式
        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码格式不正确，应为6-16位，必须包含字母和数字");
        }

        // 6. 获取当前登录用户ID
        Long userId = StpUtil.getLoginIdAsLong();

        // 7. 从数据库获取用户信息
        User user = userMapper.selectById(userId);
        if (ObjectUtil.isNull(user)) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND, "用户不存在");
        }

        // 8. 验证旧密码是否正确
        String encryptOldPassword = SaSecureUtil.md5BySalt(oldPassword, user.getSalt());
        if (!encryptOldPassword.equals(user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR, "旧密码错误");
        }

        // 9. 生成新盐值和加密新密码
        String newSalt = IdUtil.simpleUUID().substring(0, 8);
        String encryptNewPassword = SaSecureUtil.md5BySalt(newPassword, newSalt);

        // 10. 更新数据库
        LocalDateTime now = LocalDateTime.now();
        int result = userMapper.updatePassword(userId, encryptNewPassword, newSalt, now);
        if (result != 1) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "密码更新失败，请稍后重试");
        }

        // 11. 更新线程上下文中的用户信息
        User contextUser = UserContext.getUser();
        if (ObjectUtil.isNotNull(contextUser)) {
            contextUser.setPassword(encryptNewPassword);
            contextUser.setSalt(newSalt);
            contextUser.setUpdateTime(now);
            UserContext.setUser(contextUser);
        }

        // 12. 强制注销当前会话，要求重新登录
        StpUtil.logout();

        return true;
    }

    /*用户管理相关接口*/

    /**
     * 管理员添加用户
     *
     * @param userAddRequest 用户添加请求
     * @return 新创建的用户ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addUser(AdminUserAddRequest userAddRequest) {
        // 1. 参数校验（控制层已经进行基本校验，这里进行补充校验）
        String account = userAddRequest.getAccount();
        String password = userAddRequest.getPassword();

        // 2. 检查账号是否已存在
        if (bloomFilterConfig.mightContain(account)) {
            User existUser = userMapper.selectByAccount(account);
            if (ObjectUtil.isNotNull(existUser)) {
                throw new BusinessException(ErrorCode.ACCOUNT_EXIST, "账号已存在");
            }
        }

        // 3. 密码加密
        String salt = IdUtil.simpleUUID().substring(0, 8);
        String encryptPassword = SaSecureUtil.md5BySalt(password, salt);

        // 4. 构建用户对象
        LocalDateTime now = LocalDateTime.now();
        // 如果没有指定用户名，则生成一个随机用户名
        String username = userAddRequest.getUsername();
        if (StrUtil.isBlank(username)) {
            username = UsernameGenerator.generateGalleryUsername();
        }

        // 生成雪花算法ID
        Long userId = snowflakeIdGenerator.nextId();

        // 5. 角色和状态处理
        String role = userAddRequest.getRole();
        if (StrUtil.isBlank(role)) {
            // 默认为普通用户
            role = UserRoleEnum.USER.name();
        }

        String status = userAddRequest.getStatus();
        if (StrUtil.isBlank(status)) {
            // 默认为激活状态
            status = UserStatusEnum.ACTIVE.name();
        }

        // 获取当前管理员ID作为创建人
        Long adminId = StpUtil.getLoginIdAsLong();

        User user = User.builder()
                .id(userId)
                .account(account)
                .username(username)
                .password(encryptPassword)
                .salt(salt)
                .phone(userAddRequest.getPhone())
                .avatar(userAddRequest.getAvatar())
                .userProfile(userAddRequest.getUserProfile())
                .role(role)
                .status(status)
                .registerTime(now)
                .createTime(now)
                .createBy(adminId)
                .updateTime(now)
                .updateBy(adminId)
                .isDeleted(0)
                .build();

        // 6. 插入数据库
        int insertResult = userMapper.insertWithId(user);
        if (insertResult != 1) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "添加用户失败，请稍后重试");
        }

        // 7. 将账号添加到布隆过滤器
        bloomFilterConfig.addAccount(account);

        return user.getId();
    }

    /**
     * 根据ID获取用户详情
     *
     * @param id 用户ID
     * @return 用户详情
     */
    @Override
    public UserVO getUserById(Long id) {
        // 1. 参数校验
        if (ObjectUtil.isNull(id) || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不合法");
        }

        // 2. 查询用户信息
        User user = userMapper.selectById(id);
        if (ObjectUtil.isNull(user) || user.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND, "用户不存在");
        }

        // 3. 转换为VO对象（返回完整信息，因为是管理员访问）
        return UserVO.builder()
                .id(user.getId())
                .account(user.getAccount())
                .username(user.getUsername())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .userProfile(user.getUserProfile())
                .role(user.getRole())
                .status(user.getStatus())
                .lastLoginTime(user.getLastLoginTime())
                .lastLoginIp(user.getLastLoginIp())
                .registerTime(user.getRegisterTime())
                .createTime(user.getCreateTime())
                .updateTime(user.getUpdateTime())
                .remark(user.getRemark())
                .build();
    }

    /**
     * 根据ID获取脱敏用户信息
     *
     * @param id 用户ID
     * @return 脱敏用户信息
     */
    @Override
    public UserVO getUserVOById(Long id) {
        // 1. 参数校验
        if (ObjectUtil.isNull(id) || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不合法");
        }

        // 2. 查询用户信息
        User user = userMapper.selectById(id);
        if (ObjectUtil.isNull(user) || user.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND, "用户不存在");
        }

        // 3. 转换为脱敏VO对象（不包含敏感信息）
        return UserVO.builder()
                .id(user.getId())
                .account(user.getAccount())
                .username(user.getUsername())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .userProfile(user.getUserProfile())
                .role(user.getRole())
                .status(user.getStatus())
                .lastLoginTime(user.getLastLoginTime())
                .lastLoginIp(user.getLastLoginIp())
                .registerTime(user.getRegisterTime())
                .createTime(user.getCreateTime())
                .updateTime(user.getUpdateTime())
                .build();
    }

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteUser(Long id) {
        // 1. 参数校验
        if (ObjectUtil.isNull(id) || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不合法");
        }

        // 2. 检查用户是否存在
        User user = userMapper.selectById(id);
        if (ObjectUtil.isNull(user) || user.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND, "用户不存在");
        }

        // 3. 获取当前管理员ID作为更新人
        Long adminId = StpUtil.getLoginIdAsLong();
        LocalDateTime now = LocalDateTime.now();

        // 4. 逻辑删除用户（更新isDeleted字段）
        int result = userMapper.logicDeleteUser(id, now, adminId);
        if (result != 1) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除用户失败，请稍后重试");
        }

        // 5. 如果用户已登录，强制下线
        if (StpUtil.isLogin(id)) {
            StpUtil.logout(id);
        }

        return true;
    }

    /**
     * 管理员更新用户信息
     *
     * @param userUpdateRequest 用户更新请求
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateUserByAdmin(AdminUserUpdateRequest userUpdateRequest) {
        // 1. 参数校验
        if (ObjectUtil.isNull(userUpdateRequest) || ObjectUtil.isNull(userUpdateRequest.getId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        }

        Long userId = userUpdateRequest.getId();

        // 2. 检查用户是否存在
        User user = userMapper.selectById(userId);
        if (ObjectUtil.isNull(user) || user.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND, "用户不存在");
        }

        // 3. 构建更新对象
        User updateUser = new User();
        updateUser.setId(userId);

        // 只更新非空字段
        if (StrUtil.isNotBlank(userUpdateRequest.getUsername())) {
            updateUser.setUsername(userUpdateRequest.getUsername());
        }

        if (StrUtil.isNotBlank(userUpdateRequest.getPhone())) {
            updateUser.setPhone(userUpdateRequest.getPhone());
        }

        if (StrUtil.isNotBlank(userUpdateRequest.getAvatar())) {
            updateUser.setAvatar(userUpdateRequest.getAvatar());
        }


        if (StrUtil.isNotBlank(userUpdateRequest.getRole())) {
            updateUser.setRole(userUpdateRequest.getRole());
        }

        if (StrUtil.isNotBlank(userUpdateRequest.getStatus())) {
            updateUser.setStatus(userUpdateRequest.getStatus());
        }

        if (StrUtil.isNotBlank(userUpdateRequest.getRemark())) {
            updateUser.setRemark(userUpdateRequest.getRemark());
        }

        // 设置更新时间和更新人
        LocalDateTime now = LocalDateTime.now();
        Long adminId = StpUtil.getLoginIdAsLong();
        updateUser.setUpdateTime(now);
        updateUser.setUpdateBy(adminId);

        // 4. 执行更新操作
        int result = userMapper.updateByPrimaryKeySelective(updateUser);
        if (result != 1) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新用户信息失败，请稍后重试");
        }

        return true;
    }

    /**
     * 分页获取用户列表
     *
     * @param userQueryRequest 用户查询请求
     * @return 分页用户列表
     */
    @Override
    public PageResult<UserVO> listUserByPage(AdminUserQueryRequest userQueryRequest) {
        // 1. 参数校验与默认值处理
        if (ObjectUtil.isNull(userQueryRequest)) {
            userQueryRequest = new AdminUserQueryRequest();
        }

        Integer pageNum = userQueryRequest.getPageNum();
        Integer pageSize = userQueryRequest.getPageSize();

        // 设置默认值
        if (ObjectUtil.isNull(pageNum) || pageNum < 1) {
            pageNum = 1;
        }
        if (ObjectUtil.isNull(pageSize) || pageSize < 1 || pageSize > 100) {
            pageSize = 10;
        }

        // 计算分页起始位置
        int offset = (pageNum - 1) * pageSize;

        // 2. 查询总数
        Long total = userMapper.countUsers(userQueryRequest,
                userQueryRequest.getRegisterTimeStart(),
                userQueryRequest.getRegisterTimeEnd());

        // 如果没有数据，直接返回空结果
        if (total == 0) {
            return PageResult.build(0L, Collections.emptyList(), pageNum, pageSize);
        }

        // 3. 查询分页数据
        List<User> userList = userMapper.listUsersByPage(userQueryRequest,
                userQueryRequest.getRegisterTimeStart(),
                userQueryRequest.getRegisterTimeEnd(),
                offset,
                pageSize);

        // 4. 将实体列表转换为VO列表
        List<UserVO> userVOList = userList.stream().map(user -> UserVO.builder()
                .id(user.getId())
                .account(user.getAccount())
                .username(user.getUsername())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .userProfile(user.getUserProfile())
                .role(user.getRole())
                .status(user.getStatus())
                .lastLoginTime(user.getLastLoginTime())
                .lastLoginIp(user.getLastLoginIp())
                .registerTime(user.getRegisterTime())
                .createTime(user.getCreateTime())
                .updateTime(user.getUpdateTime())
                .build()).collect(Collectors.toList());

        // 5. 封装并返回分页结果
        return PageResult.build(total, userVOList, pageNum, pageSize);
    }

    /**
     * 禁用用户
     *
     * @param id 用户ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean banUser(Long id) {
        // 1. 参数校验
        if (ObjectUtil.isNull(id) || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不合法");
        }

        // 2. 检查用户是否存在
        User user = userMapper.selectById(id);
        if (ObjectUtil.isNull(user) || user.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND, "用户不存在");
        }

        // 3. 检查用户当前状态
        if (UserStatusEnum.BANNED.name().equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已经是禁用状态");
        }

        // 4. 获取当前管理员ID作为更新人
        Long adminId = StpUtil.getLoginIdAsLong();
        LocalDateTime now = LocalDateTime.now();

        // 5. 更新用户状态
        int result = userMapper.updateUserStatus(id, UserStatusEnum.BANNED.name(), now, adminId);
        if (result != 1) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "禁用用户失败，请稍后重试");
        }

        // 6. 如果用户已登录，强制下线
        if (StpUtil.isLogin(id)) {
            StpUtil.logout(id);
        }

        return true;
    }

    /**
     * 解禁用户
     *
     * @param id 用户ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean unbanUser(Long id) {
        // 1. 参数校验
        if (ObjectUtil.isNull(id) || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不合法");
        }

        // 2. 检查用户是否存在
        User user = userMapper.selectById(id);
        if (ObjectUtil.isNull(user) || user.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND, "用户不存在");
        }

        // 3. 检查用户当前状态
        if (!UserStatusEnum.BANNED.name().equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户当前不是禁用状态");
        }

        // 4. 获取当前管理员ID作为更新人
        Long adminId = StpUtil.getLoginIdAsLong();
        LocalDateTime now = LocalDateTime.now();

        // 5. 更新用户状态
        int result = userMapper.updateUserStatus(id, UserStatusEnum.ACTIVE.name(), now, adminId);
        if (result != 1) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "解禁用户失败，请稍后重试");
        }

        return true;
    }

    /**
     * 获取用户统计信息
     *
     * @return 用户统计信息
     */
    @Override
    public UserStatisticsVO getUserStatistics() {
        // -------------- 基础统计数据 --------------
        // 1. 获取注册用户总数
        Long totalUsers = userMapper.countTotalUsers();

        // 2. 获取本月新增用户数
        Long newUsersThisMonth = userMapper.countNewUsersThisMonth();

        // 3. 获取VIP用户数 (假设VIP用户角色为"VIP")
        String vipRole = UserRoleEnum.VIP.getValue();
        Long vipUsers = userMapper.countUsersByRole(vipRole);

        // 4. 计算活跃用户数量和比例 (最近30天内有登录记录的为活跃用户)
        Long activeUsers = userMapper.countActiveUsers();
        Double activeUserRatio = 0.0;
        if (ObjectUtil.isNotNull(totalUsers) && totalUsers > 0) {
            activeUserRatio = (activeUsers * 100.0) / totalUsers;
            // 保留两位小数
            activeUserRatio = Math.round(activeUserRatio * 100.0) / 100.0;
        }

        // -------------- 增长率统计 --------------
        // 5. 获取上月用户总数和计算增长率
        Long lastMonthTotalUsers = userMapper.countLastMonthTotalUsers();
        Double totalUserGrowth = 0.0;
        if (ObjectUtil.isNotNull(lastMonthTotalUsers) && lastMonthTotalUsers > 0) {
            totalUserGrowth = ((totalUsers - lastMonthTotalUsers) * 100.0) / lastMonthTotalUsers;
            totalUserGrowth = Math.round(totalUserGrowth * 100.0) / 100.0;
        }

        // 6. 获取上月新增用户数和计算增长率
        Long lastMonthNewUsers = userMapper.countNewUsersLastMonth();
        Double newUserGrowth = 0.0;
        if (ObjectUtil.isNotNull(lastMonthNewUsers) && lastMonthNewUsers > 0) {
            newUserGrowth = ((newUsersThisMonth - lastMonthNewUsers) * 100.0) / lastMonthNewUsers;
            newUserGrowth = Math.round(newUserGrowth * 100.0) / 100.0;
        }

        // 7. 获取上月VIP用户数和计算增长率
        Long lastMonthVipUsers = userMapper.countLastMonthUsersByRole(vipRole);
        Double vipUserGrowth = 0.0;
        if (ObjectUtil.isNotNull(lastMonthVipUsers) && lastMonthVipUsers > 0) {
            vipUserGrowth = ((vipUsers - lastMonthVipUsers) * 100.0) / lastMonthVipUsers;
            vipUserGrowth = Math.round(vipUserGrowth * 100.0) / 100.0;
        }

        // 8. 计算上月活跃用户比例和变化
        Long lastMonthActiveUsers = userMapper.countLastMonthActiveUsers();
        Double lastMonthActiveRatio = 0.0;
        Double activeGrowth = 0.0;
        if (ObjectUtil.isNotNull(lastMonthActiveUsers) && lastMonthTotalUsers > 0) {
            lastMonthActiveRatio = (lastMonthActiveUsers * 100.0) / lastMonthTotalUsers;
            lastMonthActiveRatio = Math.round(lastMonthActiveRatio * 100.0) / 100.0;
            // 计算活跃比例变化（百分点变化，而非增长率）
            activeGrowth = activeUserRatio - lastMonthActiveRatio;
            activeGrowth = Math.round(activeGrowth * 100.0) / 100.0;
        }

        // -------------- 额外统计数据 --------------
        // 9. 获取今日登录用户数量
        Long todayLoginUsers = userMapper.countTodayLoginUsers();

        // 10. 获取本周新增用户数量
        Long newUsersThisWeek = userMapper.countNewUsersThisWeek();

        // 11. 获取冻结账户数量
        String bannedStatus = UserStatusEnum.BANNED.getValue();
        Long bannedUsers = userMapper.countUsersByStatus(bannedStatus);

        // 12. 构建并返回统计VO
        return UserStatisticsVO.builder()
                .totalUsers(totalUsers)
                .newUsersThisMonth(newUsersThisMonth)
                .vipUsers(vipUsers)
                .activeUserRatio(activeUserRatio)
                .totalUserGrowth(totalUserGrowth)
                .newUserGrowth(newUserGrowth)
                .vipUserGrowth(vipUserGrowth)
                .activeGrowth(activeGrowth)
                .todayLoginUsers(todayLoginUsers)
                .newUsersThisWeek(newUsersThisWeek)
                .bannedUsers(bannedUsers)
                .build();
    }


    /**
     * 管理员重置用户密码
     *
     * @param userId        用户ID
     * @param newPassword   新密码
     * @param checkPassword 确认密码
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean resetUserPassword(Long userId, String newPassword, String checkPassword) {
        // 1. 参数校验
        if (ObjectUtil.isNull(userId) || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不合法");
        }

        if (StrUtil.hasBlank(newPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能为空");
        }

        // 2. 检查两次密码是否一致
        if (!newPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        // 3. 校验新密码格式
        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码格式不正确，应为6-16位，必须包含字母和数字");
        }

        // 4. 检查用户是否存在
        User user = userMapper.selectById(userId);
        if (ObjectUtil.isNull(user) || user.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND, "用户不存在");
        }

        // 5. 生成新盐值和加密新密码
        String newSalt = IdUtil.simpleUUID().substring(0, 8);
        String encryptNewPassword = SaSecureUtil.md5BySalt(newPassword, newSalt);

        // 6. 更新数据库
        LocalDateTime now = LocalDateTime.now();
        int result = userMapper.updatePassword(userId, encryptNewPassword, newSalt, now);
        if (result != 1) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "密码重置失败，请稍后重试");
        }

        // 7. 如果用户已登录，强制下线
        if (StpUtil.isLogin(userId)) {
            StpUtil.logout(userId);
        }

        return true;
    }


    /**
     * 批量删除用户
     *
     * @param ids 用户ID列表
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchDeleteUsers(List<Long> ids) {
        // 1. 参数校验
        if (ObjectUtil.isEmpty(ids)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID列表不能为空");
        }

        // 2. 获取当前管理员ID作为更新人
        Long adminId = StpUtil.getLoginIdAsLong();
        LocalDateTime now = LocalDateTime.now();

        // 3. 批量逻辑删除用户
        int result = userMapper.batchLogicDeleteUsers(ids, now, adminId);

        // 4. 批量登出用户
        for (Long id : ids) {
            if (StpUtil.isLogin(id)) {
                StpUtil.logout(id);
            }
        }

        return true;
    }

    /**
     * 根据用户ID获取用户
     *
     * @param createUser 用户ID
     * @return 用户
     */
    @Override
    public User getUsernameById(Long createUser) {
        return userMapper.getUsernameById(createUser);
    }
}