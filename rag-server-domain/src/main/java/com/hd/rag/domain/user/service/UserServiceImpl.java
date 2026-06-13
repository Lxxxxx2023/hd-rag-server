package com.hd.rag.domain.user.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.hd.rag.domain.user.adapter.repository.IUserRepository;
import com.hd.rag.domain.user.model.entity.UserEntity;
import com.hd.rag.types.errorcode.BaseErrorCode;
import com.hd.rag.types.exception.ClientException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户服务实现
 */
@Service
public class UserServiceImpl implements IUserService {

    @Resource
    private IUserRepository userRepository;

    @Override
    public UserEntity login(String username, String rawPassword) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ClientException("用户名或密码错误"));
        if (!BCrypt.checkpw(rawPassword, user.getPassword())) {
            throw new ClientException(BaseErrorCode.PASSWORD_VERIFY_ERROR);
        }
        return user;
    }

    @Override
    public UserEntity register(String username, String rawPassword) {
        userRepository.findByUsername(username).ifPresent(u -> {
            throw new ClientException(BaseErrorCode.USER_NAME_EXIST_ERROR);
        });

        UserEntity entity = new UserEntity();
        entity.setId(IdUtil.getSnowflakeNextIdStr());
        entity.setUsername(username);
        entity.setPassword(BCrypt.hashpw(rawPassword));
        entity.setCreateBy(username);
        entity.setUpdateBy(username);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        userRepository.insert(entity);
        return entity;
    }

    @Override
    public UserEntity getById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ClientException("用户不存在"));
    }
}
