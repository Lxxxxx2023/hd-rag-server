package com.hd.rag.domain.index.service.parse;

import com.hd.rag.domain.index.model.valObj.CanonicalDocument;
import com.hd.rag.domain.index.model.valObj.ContentNode;
import com.hd.rag.domain.index.model.valObj.ContentNodeType;
import com.hd.rag.domain.index.service.chain.RawData;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class PDFParserStrategy implements IParseStrategy {

    private static final float TITLE_FONT_SIZE_THRESHOLD = 14f;
    private static final float HEADER_Y_THRESHOLD = 50f;   // 距页面顶部距离，视为页眉
    private static final float FOOTER_Y_THRESHOLD = 50f;   // 距页面底部距离，视为页脚
    private static final float LINE_Y_TOLERANCE = 3f;       // Y 坐标容差，同一行判定

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
            int pageCount = pdDocument.getNumberOfPages();
            float pageHeight = pdDocument.getPage(0).getMediaBox().getHeight();

            List<ContentNode> pageNodes = new ArrayList<>();
            PageTextStripper stripper = new PageTextStripper();

            for (int i = 0; i < pageCount; i++) {
                stripper.setStartPage(i + 1);
                stripper.setEndPage(i + 1);
                stripper.setSortByPosition(true);

                // 触发 processTextPosition 逐元素回调，收集到 stripper.textPositions
                stripper.getText(pdDocument);

                List<TextPosition> positions = stripper.textPositions;
                if (positions.isEmpty()) {
                    continue;
                }

                List<ContentNode> lineNodes = groupIntoLines(positions, pageHeight);

                ContentNode pageNode = ContentNode.builder()
                        .type(ContentNodeType.PAGE)
                        .sourcePointer("page:" + (i + 1))
                        .children(lineNodes)
                        .build();
                pageNode.setMarkdown(renderMarkdown(lineNodes));
                pageNode.setPlainText(renderPlainText(lineNodes));

                pageNodes.add(pageNode);
            }

            ContentNode root = ContentNode.builder()
                    .type(ContentNodeType.PAGE)
                    .children(pageNodes)
                    .build();

            return CanonicalDocument.builder()
                    .sourceType("FILE")
                    .mimeType("application/pdf")
                    .root(root)
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("PDF文件解析失败", e);
        }
    }

    /**
     * 核心：继承 PDFTextStripper，重写 processTextPosition 收集每个文本原子。
     */
    private static class PageTextStripper extends PDFTextStripper {
        final List<TextPosition> textPositions = new ArrayList<>();

        @Override
        protected void processTextPosition(TextPosition tp) {
            textPositions.add(tp);
        }
    }

    /**
     * 将文本原子按 Y 坐标分组为行，每行为一个 TEXT_LINE 节点。
     */
    private List<ContentNode> groupIntoLines(List<TextPosition> positions, float pageHeight) {
        // 按 Y 坐标排序（页面从上到下 = Y 从大到小）
        positions.sort(Comparator.comparingDouble(TextPosition::getY).reversed()
                .thenComparingDouble(TextPosition::getX));

        List<ContentNode> lineNodes = new ArrayList<>();
        List<TextPosition> currentLine = new ArrayList<>();
        float currentY = -1;

        for (TextPosition tp : positions) {
            if (isNoise(tp, pageHeight)) {
                continue;
            }
            float y = tp.getY();
            if (currentY < 0 || Math.abs(y - currentY) <= LINE_Y_TOLERANCE) {
                currentLine.add(tp);
                currentY = currentY < 0 ? y : currentY;
            } else {
                lineNodes.add(buildLineNode(currentLine));
                currentLine.clear();
                currentLine.add(tp);
                currentY = y;
            }
        }
        if (!currentLine.isEmpty()) {
            lineNodes.add(buildLineNode(currentLine));
        }
        return lineNodes;
    }

    private ContentNode buildLineNode(List<TextPosition> line) {
        // 按 X 坐标排序（从左到右）
        line.sort(Comparator.comparingDouble(TextPosition::getX));

        String text = line.stream()
                .map(TextPosition::getUnicode)
                .reduce("", String::concat)
                .trim();

        TextPosition first = line.get(0);
        float width = line.stream().map(TextPosition::getWidth).reduce(0f, Float::sum);

        ContentNodeType type = inferType(first.getFontSize());

        return ContentNode.builder()
                .type(type)
                .sourcePointer("pos:(" + first.getX() + "," + first.getY() + ")")
                .x(first.getX())
                .y(first.getY())
                .width(width)
                .height(first.getHeight())
                .markdown(type == ContentNodeType.HEADING ? "# " + text : text)
                .plainText(text)
                .build();
    }

    /**
     * 按字号推断节点类型。大字 = 标题。
     */
    private ContentNodeType inferType(float fontSize) {
        return fontSize >= TITLE_FONT_SIZE_THRESHOLD
                ? ContentNodeType.HEADING
                : ContentNodeType.TEXT_LINE;
    }

    /**
     * 判断是否为噪音：页眉/页脚区域中字号偏小的文本。
     */
    private boolean isNoise(TextPosition tp, float pageHeight) {
        boolean inHeader = tp.getY() > pageHeight - HEADER_Y_THRESHOLD;
        boolean inFooter = tp.getY() < FOOTER_Y_THRESHOLD;
        return (inHeader || inFooter) && tp.getFontSize() < TITLE_FONT_SIZE_THRESHOLD;
    }

    private String renderMarkdown(List<ContentNode> nodes) {
        StringBuilder sb = new StringBuilder();
        for (ContentNode n : nodes) {
            if (n.getMarkdown() != null) {
                sb.append(n.getMarkdown()).append("\n");
            }
        }
        return sb.toString();
    }

    private String renderPlainText(List<ContentNode> nodes) {
        StringBuilder sb = new StringBuilder();
        for (ContentNode n : nodes) {
            if (n.getPlainText() != null) {
                sb.append(n.getPlainText()).append("\n");
            }
        }
        return sb.toString();
    }
}
