package com.hd.rag.domain.index.service.load.document;

import com.hd.rag.domain.index.service.load.document.factory.DataParserHandlerFactory;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public abstract class AbstractDocumentLoad {

    private final DataParserHandlerFactory dataParserHandlerFactory;

    private Object parse(Object o) {
        // 获取文件类型
        String type = "";
        // 获取解析器
        IDocumentLoad pdf = dataParserHandlerFactory.getParserHandler(type);
        Object parse = pdf.load(o);
        return parse;
    }

}
