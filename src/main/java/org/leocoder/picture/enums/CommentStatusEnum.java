package org.leocoder.picture.enums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author : 程序员Leo
 * @version 1.0
 * @date 2025-04-18 10:30
 * @description : 评论状态枚举
 */
@Getter
public enum CommentStatusEnum {
    PENDING("待审核", "pending"),
    APPROVED("已通过", "approved"),
    REJECTED("已拒绝", "rejected"),
    REPORTED("被举报", "reported");

    private final String text;
    private final String value;

    // 使用静态 Map 缓存所有枚举值
    private static final Map<String, CommentStatusEnum> VALUE_MAP = new HashMap<>();

    // 在静态代码块中初始化缓存
    static {
        for (CommentStatusEnum commentStatusEnum : CommentStatusEnum.values()) {
            VALUE_MAP.put(commentStatusEnum.getValue(), commentStatusEnum);
        }
    }

    CommentStatusEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值
     * @return 对应的枚举类型，找不到则返回 null
     */
    public static CommentStatusEnum getEnumByValue(String value) {
        return VALUE_MAP.get(value);
    }
    
    /**
     * 判断给定的值是否是有效的评论状态
     *
     * @param value 状态值
     * @return 是否有效
     */
    public static boolean isValidStatus(String value) {
        return VALUE_MAP.containsKey(value);
    }
}