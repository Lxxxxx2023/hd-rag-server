package com.hd.rag.infrastructure.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hd.rag.infrastructure.dao.po.DataSourcePO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据源持久层
 */
@Mapper
public interface DataSourceDao extends BaseMapper<DataSourcePO> {
}
