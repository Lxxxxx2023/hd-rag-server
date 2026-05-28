package com.hd.rag.domain.index.service.parse;

import com.hd.rag.domain.index.model.valObj.CanonicalDocument;
import com.hd.rag.domain.index.model.valObj.ContentNode;
import com.hd.rag.domain.index.model.valObj.ContentNodeType;
import com.hd.rag.domain.index.service.chain.RawData;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PDFParserStrategy implements IParseStrategy {

    private static final Logger log = LoggerFactory.getLogger(PDFParserStrategy.class);

    @Override
    public boolean canHandle(String mimeType) {
        return "application/pdf".equalsIgnoreCase(mimeType);
    }

    @Override
    public CanonicalDocument parse(RawData rawData) {
        if (rawData.getContent() == null) {
            throw new RuntimeException("PDF文件内容为空");
        }
        try (PDDocument pdDocument = Loader.loadPDF(rawData.getContent())) {
            int totalPages = pdDocument.getNumberOfPages();

            ContentNode root = ContentNode.builder()
                    .type(ContentNodeType.PAGE)
                    .plainText("")
                    .markdown("")
                    .build();

            for (int i = 0; i < totalPages; i++) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                stripper.setStartPage(i + 1);
                stripper.setEndPage(i + 1);
                String pageText = stripper.getText(pdDocument);

                if (pageText == null) {
                    pageText = "";
                }

                ContentNode pageNode = ContentNode.builder()
                        .type(ContentNodeType.PAGE)
                        .sourcePointer(String.valueOf(i + 1))
                        .plainText(pageText)
                        .markdown(pageText)
                        .build();
                root.getChildren().add(pageNode);
            }

            if (root.getChildren().isEmpty()) {
                log.warn("PDF 未提取到文本内容，可能为扫描件或图片型 PDF");
            }

            return CanonicalDocument.builder()
                    .sourceType("FILE")
                    .mimeType("application/pdf")
                    .root(root)
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("PDF文件解析失败", e);
        }
    }
}
