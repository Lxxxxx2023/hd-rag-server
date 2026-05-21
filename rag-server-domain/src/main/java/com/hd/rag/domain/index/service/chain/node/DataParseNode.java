package com.hd.rag.domain.index.service.chain.node;

import com.hd.rag.domain.index.model.aggregate.CanonicalDocumentAggregate;
import com.hd.rag.domain.index.model.entity.RawSourceDocumentEntity;
import com.hd.rag.domain.index.service.chain.DataHandleContent;
import com.hd.rag.domain.index.service.chain.DataIndexResult;
import com.hd.rag.domain.index.service.chain.IDataIndexChain;
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
        IParseStrategy parseStrategy = parserStrategyDispatch.dispatch(dataHandleContent.getMimeType());

        RawSourceDocumentEntity rawSourceDocumentEntity = RawSourceDocumentEntity.builder().build();
        CanonicalDocumentAggregate documentAggregate = parseStrategy.parse(rawSourceDocumentEntity);

        return next().handle(dataHandleContent);
    }
}
