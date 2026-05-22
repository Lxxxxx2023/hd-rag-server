package com.hd.rag.domain.index.service.chain.node;

import com.hd.rag.domain.index.model.valObj.CanonicalDocument;
import com.hd.rag.domain.index.service.chain.DataHandleContent;
import com.hd.rag.domain.index.service.chain.DataIndexResult;
import com.hd.rag.domain.index.service.chain.IDataIndexChain;
import com.hd.rag.domain.index.service.chain.RawData;
import com.hd.rag.domain.index.service.parse.IParseStrategy;
import com.hd.rag.domain.index.service.parse.ParserStrategyDispatch;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class DataParseNode implements IDataIndexChain {

    @Resource
    private DataCleanNode dataCleanNode;

    @Resource
    private ParserStrategyDispatch parserStrategyDispatch;

    @Override
    public IDataIndexChain next() {
        return dataCleanNode;
    }

    @Override
    public DataIndexResult handle(DataHandleContent dataHandleContent) throws Exception {
        RawData rawData = dataHandleContent.getRawData();

        IParseStrategy parser = parserStrategyDispatch.dispatch(rawData.getMimeType());
        CanonicalDocument canonicalDocument = parser.parse(rawData);

        dataHandleContent.setCanonicalDocument(canonicalDocument);

        return next().handle(dataHandleContent);
    }
}
