package com.unimarket.common.constant;

/**
 * 缓存常量类
 */
public class CacheConstants {

    /**
     * 用户信息缓存前缀，过期时间30分钟
     */
    public static final String USER_INFO = "user:info:";
    public static final long USER_INFO_EXPIRE = 30 * 60;

    /**
     * 用户信息缓存（区分 full / public）
     * full: 包含 IAM 权限信息，TTL 设短一些降低不一致风险
     * public: 对外公开信息，TTL 可适当长一些
     */
    public static final String USER_INFO_FULL = "user:info:full:";
    public static final long USER_INFO_FULL_EXPIRE = 60;

    public static final String USER_INFO_PUBLIC = "user:info:public:";
    public static final long USER_INFO_PUBLIC_EXPIRE = 120;

    /**
     * 关注关系检查缓存
     */
    public static final String FOLLOW_CHECK = "user:follow:check:";
    public static final long FOLLOW_CHECK_EXPIRE = 30;

    /**
     * 商品分类缓存，过期时间1天
     */
    public static final String GOODS_CATEGORY = "goods:category";
    public static final long GOODS_CATEGORY_EXPIRE = 24 * 60 * 60;

    /**
     * 热门商品缓存，过期时间1小时
     */
    public static final String HOT_GOODS = "goods:hot";
    public static final long HOT_GOODS_EXPIRE = 60 * 60;

    /**
     * 验证码缓存前缀，过期时间5分钟
     */
    public static final String VERIFY_CODE = "verify:code:";
    public static final long VERIFY_CODE_EXPIRE = 5 * 60;

    /**
     * 学校信息缓存，过期时间1天
     */
    public static final String SCHOOL_INFO = "school:info";
    public static final long SCHOOL_INFO_EXPIRE = 24 * 60 * 60;

    /**
     * Token黑名单前缀
     */
    public static final String TOKEN_BLACKLIST = "token:blacklist:";

    /**
     * Refresh Token白名单前缀（仅存在于白名单中的refresh token可用于续期）
     */
    public static final String REFRESH_TOKEN_ALLOWLIST = "token:refresh:allow:";

    /**
     * 跑腿任务位置缓存前缀，过期时间1分钟
     */
    public static final String ERRAND_LOCATION = "errand:location:";
    public static final long ERRAND_LOCATION_EXPIRE = 60;
}
