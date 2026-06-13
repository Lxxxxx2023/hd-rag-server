package com.hd.rag.document;

import com.hd.rag.domain.ingestion.model.valobj.ChunkOptions;
import com.hd.rag.domain.ingestion.model.valobj.TextChunk;
import com.hd.rag.domain.ingestion.model.valobj.VectorChunk;
import com.hd.rag.domain.ingestion.service.chunker.FixedChunkStrategy;
import com.hd.rag.domain.ingestion.service.embedder.IEmbeddingService;
import com.hd.rag.domain.ingestion.service.parser.IDocumentParser;
import com.hd.rag.domain.ingestion.service.parser.ParserRegistry;
import com.hd.rag.domain.ingestion.model.valobj.TextSegment;
import com.hd.rag.infrastructure.adapter.repository.PGVectorIndexWriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@SpringBootTest
class IndexPipelineTest {

    @Resource
    private PGVectorIndexWriter vectorStoreService;

    @Resource
    private ParserRegistry dispatch;

    @Resource
    private FixedChunkStrategy fixedChunkStrategy;

    @Resource
    IEmbeddingService embeddingService;

    private static final String MARKDOWN_STR = """


            """;
    @Test
    void markdown_parse_chunk_embed_and_store() throws Exception {
        byte[] bytes = MARKDOWN_STR.getBytes(StandardCharsets.UTF_8);
        IDocumentParser documentParser = dispatch.dispatch("text/markdown", bytes);

        List<TextSegment> parse = documentParser.parse(bytes);

        List<TextChunk> chunk = fixedChunkStrategy.chunk(parse, new ChunkOptions(512, 30));

        List<VectorChunk> embed = embeddingService.embed(chunk);
        vectorStoreService.batchInsert(embed);
    }
}
