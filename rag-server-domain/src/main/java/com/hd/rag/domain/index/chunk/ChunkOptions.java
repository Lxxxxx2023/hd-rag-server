package com.hd.rag.domain.index.chunk;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChunkOptions{

    /**
     * 切块大小
     */
    private Integer chunkSize;

    /**
     * 切块重叠部分
     */
    private Integer overlapSize;
}
