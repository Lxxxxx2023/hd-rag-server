package com.hd.rag.api;

import com.hd.rag.api.dto.KnowledgeCreateReqDTO;
import com.hd.rag.api.dto.KnowledgeDatasourceConfigCreateReqDTO;
import com.hd.rag.api.dto.KnowledgeLoadDataReqDTO;
import org.springframework.web.multipart.MultipartFile;

public interface KnowledgeServer {

    /**
     * 创建知识库
     * @param reqDTO 请求参数
     */
    void createKnowledge(KnowledgeCreateReqDTO reqDTO);
    /**
     * 加载本地文件到知识库中
     * @param multipartFile 文件
     * @param kbId 知识库id
     */
    void loadDataByFile(MultipartFile multipartFile, String kbId);

    /**
     * 创建知识库数据源
     * @param reqDTO 请求参数
     */
    void createKnowledgeDatasourceConfig(KnowledgeDatasourceConfigCreateReqDTO reqDTO);

    /**
     * 选择数据源 手动触发 加载数据到知识库
     * @param reqDTO 请求参数
     */
    void loadData(KnowledgeLoadDataReqDTO reqDTO);
}
