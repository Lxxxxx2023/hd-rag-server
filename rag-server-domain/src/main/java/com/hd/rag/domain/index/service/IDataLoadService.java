package com.hd.rag.domain.index.service;

import com.hd.rag.api.dto.KnowledgeLoadDataReqDTO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 数据加载服务接口
 */
public interface IDataLoadService {

    /**
     * 加载本地文件数据到知识库中
     * @param multipartFile 文件
     * @param kbId 知识库id
     */
    void loadDataByFile(MultipartFile multipartFile, String kbId) throws Exception;

    /**
     * 根据数据源 手动触发数据加载
     * @param reqDTO 知识库加载数据 请求参数
     */
    void loadData(KnowledgeLoadDataReqDTO reqDTO);
}
