package com.hd.rag.domain.index.vector;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VectorChunk {

    private String chunkId;

    private String content;

    private List<String> sourcePointers;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    @JsonIgnore
    private float[] embedding;

}
