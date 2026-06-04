package com.hd.rag.document;

import com.hd.rag.domain.index.chunk.ChunkOptions;
import com.hd.rag.domain.index.chunk.FixedChunkStrategy;
import com.hd.rag.domain.index.chunk.TextChunk;
import com.hd.rag.domain.index.parse.DocumentParser;
import com.hd.rag.domain.index.parse.DocumentParserDispatch;
import com.hd.rag.domain.index.parse.TextSegment;
import com.hd.rag.domain.index.vector.EmbeddingService;
import com.hd.rag.domain.index.vector.PGVectorStoreServiceImpl;
import com.hd.rag.domain.index.vector.VectorChunk;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@SpringBootTest
class IndexPipelineTest {

    @Resource
    private PGVectorStoreServiceImpl vectorStoreService;

    @Resource
    private DocumentParserDispatch dispatch;

    @Resource
    private FixedChunkStrategy fixedChunkStrategy;

    @Resource
    EmbeddingService embeddingService;

    private static final String MARKDOWN_STR = """
        
         
            """;
    @Test
    void markdown_parse_chunk_embed_and_store() throws Exception {
        byte[] bytes = MARKDOWN_STR.getBytes(StandardCharsets.UTF_8);
        DocumentParser documentParser = dispatch.dispatch("text/markdown", bytes);

        List<TextSegment> parse = documentParser.parse(bytes);

        List<TextChunk> chunk = fixedChunkStrategy.chunk(parse, new ChunkOptions(512, 30));

        List<VectorChunk> embed = embeddingService.embed(chunk);
        vectorStoreService.batchInsert(embed);
    }
}
