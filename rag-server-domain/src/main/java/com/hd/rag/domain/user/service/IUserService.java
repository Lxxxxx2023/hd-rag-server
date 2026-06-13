package com.hd.rag.domain.user.service;

import com.hd.rag.domain.user.model.entity.UserEntity;

/**
 * 用户服务接口
 */
public interface IUserService {

    /**
     * 用户登录：验证用户名密码，返回用户实体
     *
     * @param username    用户名
     * @param rawPassword 明文密码
     * @return 用户实体
     */
    UserEntity login(String username, String rawPassword);

    /**
     * 用户注册
     *
     * @param username    用户名
     * @param rawPassword 明文密码
     * @return 用户实体
     */
    UserEntity register(String username, String rawPassword);

    /**
     * 根据ID查询用户
     *
     * @param id 用户id
     * @return 用户实体
     */
    UserEntity getById(String id);
}
