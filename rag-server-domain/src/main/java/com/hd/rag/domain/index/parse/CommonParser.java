package com.hd.rag.domain.index.parse;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class CommonParser implements DocumentParser{
    private static final Tika TIKA = new Tika();
    @Override
    public ParserEnum getType() {
        return ParserEnum.COMMON;
    }

    @Override
    public boolean canHandle(String mimeType) {
        return false;
    }

    @Override
    public List<TextSegment> parse(byte[] content) {
        try {
            String rawText = TIKA.parseToString(new ByteArrayInputStream(content));
            return List.of(
                    TextSegment.builder()
                            .text(rawText)
                            .segmentType(SegmentType.PARAGRAPH)
                            .build()
            );
        } catch (IOException | TikaException e) {
            log.error("Tika 解析失败", e);
            throw new RuntimeException(e);
        }
    }
}
