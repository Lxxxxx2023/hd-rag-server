package com.hd.rag.api.user.dto.response;

/**
 * 登录/注册响应参数
 *
 * @param token    JWT Token
 * @param userId   用户id
 * @param username 用户名
 */
public record UserLoginRespDTO(
        String token,
        String userId,
        String username) {
}
