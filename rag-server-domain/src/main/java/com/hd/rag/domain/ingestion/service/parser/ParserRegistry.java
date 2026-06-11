package com.hd.rag.domain.ingestion.service.parser;

import lombok.AllArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AllArgsConstructor
public class ParserRegistry {

    private CommonParser commonParser;

    private Map<String, IDocumentParser> documentParseMap;

    private static final Tika TIKA = new Tika();

    public IDocumentParser dispatch(String mimeType, byte[] content) {
        String fileType = TIKA.detect(content);

        for (Map.Entry<String, IDocumentParser> documentParseEntry : documentParseMap.entrySet()) {
            IDocumentParser parser = documentParseEntry.getValue();
            if (parser.canHandle(fileType) || parser.canHandle(mimeType)) {
                return parser;
            }
        }
        return commonParser;
    }
}
