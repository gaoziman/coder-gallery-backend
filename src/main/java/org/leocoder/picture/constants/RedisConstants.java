package org.leocoder.picture.constants;

/**
 * @author : 程序员Leo
 * @date 2025-04-17 22:15
 * @version 1.0
 * @description : Redis键常量类
 */
public class RedisConstants {

    /**
     * 点赞/收藏计数Redis键前缀
     */
    public static final String REACTION_COUNT_PREFIX = "reaction:count:";

    /**
     * 用户点赞/收藏状态Redis键前缀
     */
    public static final String USER_REACTION_PREFIX = "user:reaction:";

    /**
     * 热门图片Redis键前缀
     */
    public static final String HOT_PICTURES_PREFIX = "hot:pictures:";

    /**
     * 缓存过期时间（天）- 计数缓存
     */
    public static final long COUNT_CACHE_EXPIRE_DAYS = 1;

    /**
     * 缓存过期时间（天）- 用户状态缓存
     */
    public static final long USER_CACHE_EXPIRE_DAYS = 2;

    /**
     * 缓存过期时间（分钟）- 热门图片缓存
     */
    public static final long HOT_CACHE_EXPIRE_MINUTES = 30;

    /**
     * 点赞类型常量
     */
    public static final String REACTION_LIKE = "like";

    /**
     * 收藏类型常量
     */
    public static final String REACTION_FAVORITE = "favorite";

    /**
     * 浏览类型常量
     */
    public static final String REACTION_VIEW = "view";

    /**
     * 图片目标类型常量
     */
    public static final String TARGET_PICTURE = "picture";

    /**
     * 生成点赞/收藏计数的Redis键
     */
    public static String getReactionCountKey(String targetType, Long targetId, String reactionType) {
        return REACTION_COUNT_PREFIX + targetType + ":" + targetId + ":" + reactionType;
    }

    /**
     * 生成用户点赞/收藏状态的Redis键
     */
    public static String getUserReactionKey(String targetType, Long targetId, Long userId) {
        return USER_REACTION_PREFIX + userId + ":" + targetType + ":" + targetId;
    }

    /**
     * 生成热门图片的Redis键
     */
    public static String getHotPicturesKey(String period) {
        return HOT_PICTURES_PREFIX + period;
    }
}