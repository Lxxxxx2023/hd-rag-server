package com.hd.rag.infrastructure.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hd.rag.infrastructure.dao.po.PipelinePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 流水线持久层
 */
@Mapper
public interface PipelineDao extends BaseMapper<PipelinePO> {
}
