package com.hd.rag.domain.ingestion.service.parser;

import com.hd.rag.domain.ingestion.model.valobj.ParserType;
import com.hd.rag.domain.ingestion.model.valobj.SegmentType;
import com.hd.rag.domain.ingestion.model.valobj.TextSegment;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class CommonParser implements IDocumentParser {
    @Override
    public ParserType getType() {
        return ParserType.COMMON;
    }

    @Override
    public boolean canHandle(String mimeType) {
        return true;
    }

    @Override
    public List<TextSegment> parse(byte[] content) {
        Tika tika = new Tika();
        try {
            String text = tika.parseToString(new ByteArrayInputStream(content));
            return List.of(TextSegment.builder()
                    .segmentType(SegmentType.PARAGRAPH)
                    .text(text)
                    .build());
        } catch (IOException | TikaException e) {
            log.error("CommonParser parse error", e);
            return List.of();
        }
    }
}
