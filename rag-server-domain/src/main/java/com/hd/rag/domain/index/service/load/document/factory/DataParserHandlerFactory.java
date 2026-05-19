package com.hd.rag.domain.index.service.load.document.factory;

import com.hd.rag.domain.index.service.load.document.IDocumentLoad;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 解析器工厂
 */
@Service
public class DataParserHandlerFactory {

    private final Map<String, IDocumentLoad> documentParserHandlerMap;

    public DataParserHandlerFactory(Map<String, IDocumentLoad> documentParserHandlerMap) {
        this.documentParserHandlerMap = documentParserHandlerMap;
    }

    public IDocumentLoad getParserHandler(String type) {
        return documentParserHandlerMap.get("type");
    }
}
