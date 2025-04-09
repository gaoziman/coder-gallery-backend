package org.leocoder.picture.enums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-08 14:32
 * @description : 分类状态枚举
 */
@Getter
public enum CategoryStatusEnum {

    ACTIVE("激活", "active"),

    DISABLED("禁用", "disabled");

    private final String text;
    private final String value;

    // 使用静态 Map 缓存所有枚举值
    private static final Map<String, CategoryStatusEnum> VALUE_MAP = new HashMap<>();

    // 在静态代码块中初始化缓存
    static {
        for (CategoryStatusEnum statusEnum : CategoryStatusEnum.values()) {
            VALUE_MAP.put(statusEnum.getValue(), statusEnum);
        }
    }

    CategoryStatusEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值
     * @return 对应的枚举类型，找不到则返回 null
     */
    public static CategoryStatusEnum getEnumByValue(String value) {
        return VALUE_MAP.get(value);
    }
}