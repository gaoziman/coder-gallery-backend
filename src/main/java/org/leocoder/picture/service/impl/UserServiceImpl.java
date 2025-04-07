package org.leocoder.picture.service.impl;

import cn.dev33.satoken.secure.SaSecureUtil;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.leocoder.picture.config.BloomFilterConfig;
import org.leocoder.picture.domain.pojo.User;
import org.leocoder.picture.domain.vo.user.LoginUserVO;
import org.leocoder.picture.domain.vo.user.UserVO;
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
import java.util.regex.Pattern;

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
            if (existUser != null) {
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
                .role("user")
                // 默认状态为已激活
                .status("active")
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
        if (user == null) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND, "账号不存在");
        }

        // 4. 状态校验
        if (!"active".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_BANNED, "账号状态异常，请联系管理员");
        }

        // 5. 密码校验
        String encryptPassword = SaSecureUtil.md5BySalt(userPassword, user.getSalt());
        if (!encryptPassword.equals(user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR, "密码错误");
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
        if (user == null) {
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
}