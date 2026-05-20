package com.hd.rag.domain.index.service.impl;

import com.hd.rag.api.dto.KnowledgeLoadDataReqDTO;
import com.hd.rag.domain.index.service.IDataLoadService;
import com.hd.rag.domain.index.service.parse.ParserRegistry;
import jakarta.annotation.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * 数据加载服务实现类
 */
public class DataLoadServiceImpl implements IDataLoadService {

    @Resource
    private ParserRegistry registry;

    @Override
    public void loadDataByFile(MultipartFile multipartFile, String kbId) {
        // 1. 解析文件类型
        // 2. 选择对应解析器
        // 3. 数据处理通用流程
    }

    @Override
    public void loadData(KnowledgeLoadDataReqDTO reqDTO) {
    }
}
