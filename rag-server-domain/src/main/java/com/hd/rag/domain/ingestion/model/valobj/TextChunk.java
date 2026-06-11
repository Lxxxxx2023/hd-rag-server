package com.hd.rag.domain.ingestion.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TextChunk {

    private String content;

    private List<String> sourcePointers;

}
