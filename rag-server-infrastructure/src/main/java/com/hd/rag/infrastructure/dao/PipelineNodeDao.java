package com.hd.rag.infrastructure.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hd.rag.infrastructure.dao.po.PipelineNodePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 流水线节点持久层
 */
@Mapper
public interface PipelineNodeDao extends BaseMapper<PipelineNodePO> {
}
