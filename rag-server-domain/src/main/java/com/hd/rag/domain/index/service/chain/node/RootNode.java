package com.hd.rag.domain.index.service.chain.node;

import com.hd.rag.domain.index.service.chain.DataHandleContent;
import com.hd.rag.domain.index.service.chain.DataIndexResult;
import com.hd.rag.domain.index.service.chain.IDataIndexChain;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

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

        return next().handle(dataHandleContent);
    }
}
