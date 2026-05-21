package com.hd.rag.domain.index.model.aggregate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CanonicalDocumentAggregate {

    private String content;

    private List<String> chunks;

    private List<float[]> embeddings;
}
