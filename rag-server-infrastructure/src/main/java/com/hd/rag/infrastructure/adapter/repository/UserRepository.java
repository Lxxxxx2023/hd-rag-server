package com.hd.rag.infrastructure.adapter.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hd.rag.domain.user.adapter.repository.IUserRepository;
import com.hd.rag.domain.user.model.entity.UserEntity;
import com.hd.rag.infrastructure.dao.UserDao;
import com.hd.rag.infrastructure.dao.po.UserPO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户仓储适配器
 */
@Repository
public class UserRepository implements IUserRepository {

    @Resource
    private UserDao userDao;

    @Override
    public void insert(UserEntity entity) {
        userDao.insert(toPO(entity));
    }

    @Override
    public Optional<UserEntity> findById(String id) {
        return Optional.ofNullable(userDao.selectById(id)).map(this::toDomain);
    }

    @Override
    public Optional<UserEntity> findByUsername(String username) {
        LambdaQueryWrapper<UserPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPO::getUsername, username);
        return Optional.ofNullable(userDao.selectOne(wrapper)).map(this::toDomain);
    }

    private UserPO toPO(UserEntity entity) {
        UserPO po = new UserPO();
        po.setId(entity.getId());
        po.setUsername(entity.getUsername());
        po.setPassword(entity.getPassword());
        po.setCreateBy(entity.getCreateBy());
        po.setCreateTime(entity.getCreateTime());
        po.setUpdateBy(entity.getUpdateBy());
        po.setUpdateTime(entity.getUpdateTime());
        return po;
    }

    private UserEntity toDomain(UserPO po) {
        UserEntity entity = new UserEntity();
        entity.setId(po.getId());
        entity.setUsername(po.getUsername());
        entity.setPassword(po.getPassword());
        entity.setCreateBy(po.getCreateBy());
        entity.setCreateTime(po.getCreateTime());
        entity.setUpdateBy(po.getUpdateBy());
        entity.setUpdateTime(po.getUpdateTime());
        return entity;
    }
}
