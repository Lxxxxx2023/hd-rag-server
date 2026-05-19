package com.hd.rag.domain.index.service.load.document.handler;

import com.hd.rag.domain.index.service.load.document.IDocumentLoad;
import org.springframework.stereotype.Component;

@Component
public class MarkdownLoadHandler implements IDocumentLoad {
    @Override
    public String getType() {
        return "";
    }

    @Override
    public Object load(Object o) {
        return null;
    }
}
