package org.leocoder.picture.enums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-09 11:15
 * @description : 标签状态枚举
 */
@Getter
public enum TagStatusEnum {
    ACTIVE("已启用", "active"),

    DISABLED("已禁用", "inactive");

    private final String text;
    private final String value;

    // 使用静态 Map 缓存所有枚举值
    private static final Map<String, TagStatusEnum> VALUE_MAP = new HashMap<>();

    // 在静态代码块中初始化缓存
    static {
        for (TagStatusEnum statusEnum : TagStatusEnum.values()) {
            VALUE_MAP.put(statusEnum.getValue(), statusEnum);
        }
    }

    TagStatusEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值
     * @return 对应的枚举类型，找不到则返回 null
     */
    public static TagStatusEnum getEnumByValue(String value) {
        return VALUE_MAP.get(value);
    }

    /**
     * 校验状态值是否有效
     *
     * @param value 状态值
     * @return 是否有效
     */
    public static boolean isValid(String value) {
        return getEnumByValue(value) != null;
    }
}