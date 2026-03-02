package com.unimarket.security;

/**
 * 用户上下文持有者
 * 使用ThreadLocal存储当前请求的用户上下文信息
 */
public class UserContextHolder {

    private static final ThreadLocal<UserContext> contextHolder = new ThreadLocal<>();

    /**
     * 设置当前用户上下文
     * @param context 用户上下文
     */
    public static void setContext(UserContext context) {
        contextHolder.set(context);
    }

    /**
     * 获取当前用户上下文
     * @return 用户上下文,如果未设置则返回null
     */
    public static UserContext getContext() {
        return contextHolder.get();
    }

    /**
     * 获取当前用户ID
     * @return 用户ID,如果未设置则返回null
     */
    public static Long getUserId() {
        UserContext context = getContext();
        return context != null ? context.getUserId() : null;
    }

    /**
     * 获取当前用户学号
     * @return 学号,如果未设置则返回null
     */
    public static String getStudentNo() {
        UserContext context = getContext();
        return context != null ? context.getStudentNo() : null;
    }

    /**
     * 获取当前用户学校编码
     * @return 学校编码,如果未设置则返回null
     */
    public static String getSchoolCode() {
        UserContext context = getContext();
        return context != null ? context.getSchoolCode() : null;
    }

    /**
     * 获取当前用户校区编码
     * @return 校区编码,如果未设置则返回null
     */
    public static String getCampusCode() {
        UserContext context = getContext();
        return context != null ? context.getCampusCode() : null;
    }

    /**
     * 获取当前用户认证状态
     * @return 认证状态,如果未设置则返回null
     */
    public static Integer getAuthStatus() {
        UserContext context = getContext();
        return context != null ? context.getAuthStatus() : null;
    }

    /**
     * 获取当前选择的校区编码（用于手动切换校区）
     * @return 选择的校区编码,如果未设置则返回用户默认校区
     */
    public static String getSelectedCampusCode() {
        UserContext context = getContext();
        if (context == null) {
            return null;
        }
        return context.getSelectedCampusCode() != null ? context.getSelectedCampusCode() : context.getCampusCode();
    }

    /**
     * 设置当前选择的校区编码
     * @param campusCode 校区编码
     */
    public static void setSelectedCampusCode(String campusCode) {
        UserContext context = getContext();
        if (context != null) {
            context.setSelectedCampusCode(campusCode);
        }
    }

    /**
     * 判断当前用户是否已认证（authStatus=2）
     * @return 是否已认证
     */
    public static boolean isAuthenticated() {
        Integer authStatus = getAuthStatus();
        return authStatus != null && authStatus == 2;
    }

    /**
     * 判断当前是否为游客（未登录或未认证）
     * @return 是否为游客
     */
    public static boolean isGuest() {
        return getUserId() == null || !isAuthenticated();
    }

    /**
     * 清除当前用户上下文
     * 必须在请求结束时调用,防止内存泄漏
     */
    public static void clear() {
        contextHolder.remove();
    }
}
