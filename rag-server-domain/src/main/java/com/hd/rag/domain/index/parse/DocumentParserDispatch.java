package com.hd.rag.domain.index.parse;

import lombok.AllArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AllArgsConstructor
public class DocumentParserDispatch {

    private CommonParser commonParser;

    private Map<String, DocumentParser> documentParseMap;

    private static final Tika TIKA = new Tika();

    public DocumentParser dispatch(String mimeType, byte[] content) {
        String fileType = TIKA.detect(content);

        for (Map.Entry<String, DocumentParser> documentParseEntry : documentParseMap.entrySet()) {
            DocumentParser parser = documentParseEntry.getValue();
            if (parser.canHandle(fileType) && parser.canHandle(mimeType)) {
                return parser;
            }
        }
        return commonParser;
    }
}
