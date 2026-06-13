package com.hd.rag.infrastructure.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hd.rag.infrastructure.dao.po.UserPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 DAO
 */
@Mapper
public interface UserDao extends BaseMapper<UserPO> {
}
