package com.hd.rag.domain.user.adapter.repository;

import com.hd.rag.domain.user.model.entity.UserEntity;

import java.util.Optional;

/**
 * 用户仓储端口
 */
public interface IUserRepository {

    /**
     * 新增用户
     */
    void insert(UserEntity entity);

    /**
     * 根据ID查询
     */
    Optional<UserEntity> findById(String id);

    /**
     * 根据用户名查询
     */
    Optional<UserEntity> findByUsername(String username);
}
