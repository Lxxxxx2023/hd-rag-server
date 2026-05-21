package com.hd.rag.domain.index.service.parse;

import com.hd.rag.domain.index.model.aggregate.CanonicalDocumentAggregate;
import com.hd.rag.domain.index.model.entity.RawSourceDocumentEntity;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PDFParserStrategy implements IParseStrategy{
    @Override
    public boolean canHandle(String mimeType) {
        return false;
    }

    @Override
    public CanonicalDocumentAggregate parse(RawSourceDocumentEntity rawSourceDocumentEntity) {

        if (rawSourceDocumentEntity.getRawContent() != null) {
            try {
                PDDocument pdDocument = Loader.loadPDF(rawSourceDocumentEntity.getRawContent());
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(pdDocument);

                return CanonicalDocumentAggregate.builder()
                        .content(text)
                        .build();
            } catch (IOException e) {
                throw new RuntimeException("PDF文件解析失败", e);
            }
        }
        throw new RuntimeException("PDF文件内容为空");
    }
}
