package com.hd.rag.types.context;

/**
 * 用户上下文 — 当前请求的用户信息
 *
 * @param userId   用户id
 * @param username 用户名
 */
public record UserContext(String userId, String username) {

    /** 未登录时的默认用户 */
    public static final String SYSTEM_USER_ID = "SYSTEM";

    /** 未登录时的系统上下文 */
    public static final UserContext SYSTEM = new UserContext(SYSTEM_USER_ID, SYSTEM_USER_ID);
}
