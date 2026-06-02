package com.hd.rag.domain.index.parse;

import java.util.List;

public interface DocumentParser {

    ParserEnum getType();

    boolean canHandle(String mimeType);

    List<TextSegment> parse(byte[] content);
}
