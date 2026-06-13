package com.hd.rag.infrastructure.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hd.rag.infrastructure.dao.po.KnowledgeBasePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库持久层
 */
@Mapper
public interface KnowledgeBaseDao extends BaseMapper<KnowledgeBasePO> {
}
