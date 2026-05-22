package com.hd.rag.domain.index.service.impl;

import com.hd.rag.api.dto.KnowledgeLoadDataReqDTO;

import com.hd.rag.domain.index.service.IDataLoadService;

import com.hd.rag.domain.index.service.chain.DataHandleContent;
import com.hd.rag.domain.index.service.chain.DefaultDataHandleFactory;
import com.hd.rag.domain.index.service.chain.RawData;
import jakarta.annotation.Resource;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 数据加载服务实现类
 */
@Service
public class DataLoadServiceImpl implements IDataLoadService {

    @Resource
    private DefaultDataHandleFactory defaultDataIndexFactory;

    @Override
    public void loadDataByFile(MultipartFile multipartFile, String kbId) throws Exception {

        Tika tika = new Tika();

        RawData rawData = RawData.builder()
                .content(multipartFile.getBytes())
                .mimeType(tika.detect(multipartFile.getInputStream()))
                .build();

        DataHandleContent dataHandleContent = new DataHandleContent();
        dataHandleContent.setRawData(rawData);
        defaultDataIndexFactory.handleData(dataHandleContent);
    }

    @Override
    public void loadData(KnowledgeLoadDataReqDTO reqDTO) {
    }
}
