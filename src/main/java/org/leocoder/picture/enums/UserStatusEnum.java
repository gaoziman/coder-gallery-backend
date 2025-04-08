package org.leocoder.picture.enums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-07 20:40
 * @description :
 */
@Getter
public enum UserStatusEnum {
    ACTIVE("已激活", "active"),
    INACTIVE("未激活", "inactive"),
    BANNED("已禁用)", "banned");

    private final String text;
    private final String value;

    // 使用静态 Map 缓存所有枚举值
    private static final Map<String, UserStatusEnum> VALUE_MAP = new HashMap<>();

    // 在静态代码块中初始化缓存
    static {
        for (UserStatusEnum userStatusEnum : UserStatusEnum.values()) {
            VALUE_MAP.put(userStatusEnum.getValue(), userStatusEnum);
        }
    }

    UserStatusEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值
     * @return 对应的枚举类型，找不到则返回 null
     */
    public static UserStatusEnum getEnumByValue(String value) {
        return VALUE_MAP.get(value);
    }
}
