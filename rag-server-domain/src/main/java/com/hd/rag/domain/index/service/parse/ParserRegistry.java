package com.hd.rag.domain.index.service.parse;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AllArgsConstructor
public class ParserRegistry {

    Map<String, IParseStrategy> parseStrategyMap;

    public IParseStrategy route(String type) {
        return parseStrategyMap.get(type);
    }
}
