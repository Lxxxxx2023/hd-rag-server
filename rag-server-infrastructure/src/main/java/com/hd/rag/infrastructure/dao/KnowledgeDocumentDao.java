package com.hd.rag.infrastructure.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hd.rag.infrastructure.dao.po.KnowledgeDocumentPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库文档持久层
 */
@Mapper
public interface KnowledgeDocumentDao extends BaseMapper<KnowledgeDocumentPO> {
}
