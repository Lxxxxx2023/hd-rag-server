package com.hd.rag.domain.index.service.chain.node;

import com.hd.rag.domain.index.service.chain.DataHandleContent;
import com.hd.rag.domain.index.service.chain.DataIndexResult;
import com.hd.rag.domain.index.service.chain.IDataIndexChain;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RootNode implements IDataIndexChain {

    @Resource
    private DataParseNode dataParseNode;

    @Override
    public IDataIndexChain next() {
        return dataParseNode;
    }

    @Override
    public DataIndexResult handle(DataHandleContent dataHandleContent) throws Exception {
        log.info("ETL RootNode 开始处理数据 ..........");
        return next().handle(dataHandleContent);
    }
}
