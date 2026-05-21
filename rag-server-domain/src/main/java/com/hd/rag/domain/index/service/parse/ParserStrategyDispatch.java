package com.hd.rag.domain.index.service.parse;

import com.hd.rag.domain.index.model.entity.RawSourceDocumentEntity;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AllArgsConstructor
public class ParserStrategyDispatch {

    Map<String, IParseStrategy> parseStrategyMap;

    public IParseStrategy dispatch(String mimeType) throws Exception {
        for (Map.Entry<String, IParseStrategy> parseStrategyEntry : parseStrategyMap.entrySet()) {
            IParseStrategy parseStrategy = parseStrategyEntry.getValue();
            if(parseStrategy.canHandle(mimeType)) {
                return parseStrategy;
            }
        }
        throw new Exception("当前不支持处理类型的数据");
    }
}
