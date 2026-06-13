package com.hd.rag.api.user.dto.response;

import java.time.LocalDateTime;

/**
 * 当前用户信息响应参数
 *
 * @param id         用户id
 * @param username   用户名
 * @param createTime 创建时间
 * @param updateTime 更新时间
 */
public record UserInfoRespDTO(
        String id,
        String username,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}
