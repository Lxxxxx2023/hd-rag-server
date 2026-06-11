package com.hd.rag.domain.ingestion.service.parser;

import com.hd.rag.domain.ingestion.model.valobj.ParserType;
import com.hd.rag.domain.ingestion.model.valobj.TextSegment;

import java.util.List;

public interface IDocumentParser {

    ParserType getType();

    boolean canHandle(String mimeType);

    List<TextSegment> parse(byte[] content);
}
