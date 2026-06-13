package com.hd.rag.api.user.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求参数
 *
 * @param username 用户名
 * @param password 密码
 */
public record UserLoginReqDTO(
        @NotBlank(message = "用户名不能为空")
        String username,

        @NotBlank(message = "密码不能为空")
        String password) {
}
