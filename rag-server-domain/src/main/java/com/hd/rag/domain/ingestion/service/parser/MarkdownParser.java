package com.hd.rag.domain.ingestion.service.parser;

import com.hd.rag.domain.ingestion.model.valobj.ParserType;
import com.hd.rag.domain.ingestion.model.valobj.SegmentType;
import com.hd.rag.domain.ingestion.model.valobj.TextSegment;
import com.vladsch.flexmark.ast.BlockQuote;
import com.vladsch.flexmark.ast.BulletList;
import com.vladsch.flexmark.ast.BulletListItem;
import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.Emphasis;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.HardLineBreak;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.HtmlEntity;
import com.vladsch.flexmark.ast.HtmlInline;
import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.ast.IndentedCodeBlock;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.ast.OrderedList;
import com.vladsch.flexmark.ast.OrderedListItem;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.SoftLineBreak;
import com.vladsch.flexmark.ast.StrongEmphasis;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.ext.tables.TableBlock;
import com.vladsch.flexmark.ext.tables.TableBody;
import com.vladsch.flexmark.ext.tables.TableCell;
import com.vladsch.flexmark.ext.tables.TableHead;
import com.vladsch.flexmark.ext.tables.TableRow;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class MarkdownParser implements IDocumentParser {
    @Override
    public ParserType getType() {
        return ParserType.MARKDOWN;
    }

    @Override
    public boolean canHandle(String mimeType) {
        return "text/markdown".equals(mimeType) || "text/x-markdown".equals(mimeType);
    }

    @Override
    public List<TextSegment> parse(byte[] content) {
        String markdown = new String(content, StandardCharsets.UTF_8);
        Parser parser = Parser.builder()
                .extensions(List.of(TablesExtension.create()))
                .build();
        Document document = parser.parse(markdown);
        List<TextSegment> segments = new ArrayList<>();
        extractSegments(document, segments);
        return segments;
    }

    private void extractSegments(Node node, List<TextSegment> segments) {
        while (node != null) {
            if (node instanceof Heading heading) {
                TextSegment seg = TextSegment.builder()
                        .segmentType(SegmentType.HEADING)
                        .level(heading.getLevel())
                        .text(heading.getText().toString())
                        .build();
                segments.add(seg);
            } else if (node instanceof Paragraph) {
                String text = extractText(node);
                if (!text.isBlank()) {
                    TextSegment seg = TextSegment.builder()
                            .segmentType(SegmentType.PARAGRAPH)
                            .level(0)
                            .text(text.trim())
                            .build();
                    segments.add(seg);
                }
            } else if (node instanceof FencedCodeBlock codeBlock) {
                String text = codeBlock.getContentChars().toString();
                TextSegment seg = TextSegment.builder()
                        .segmentType(SegmentType.CODE)
                        .level(0)
                        .text(text.trim())
                        .build();
                segments.add(seg);
            } else if (node instanceof IndentedCodeBlock codeBlock) {
                String text = codeBlock.getContentChars().toString();
                TextSegment seg = TextSegment.builder()
                        .segmentType(SegmentType.CODE)
                        .level(0)
                        .text(text.trim())
                        .build();
                segments.add(seg);
            } else if (node instanceof BulletList || node instanceof OrderedList) {
                extractListItems(node, segments);
            } else if (node instanceof TableBlock tableBlock) {
                StringBuilder tableText = new StringBuilder();
                Node child = tableBlock.getFirstChild();
                while (child != null) {
                    if (child instanceof TableHead || child instanceof TableBody) {
                        Node row = child.getFirstChild();
                        while (row != null) {
                            if (row instanceof TableRow) {
                                Node cell = row.getFirstChild();
                                while (cell != null) {
                                    if (cell instanceof TableCell) {
                                        tableText.append(cell.getChars().toString().trim());
                                        tableText.append("\t");
                                    }
                                    cell = cell.getNext();
                                }
                                tableText.append("\n");
                            }
                            row = row.getNext();
                        }
                    }
                    child = child.getNext();
                }
                TextSegment seg = TextSegment.builder()
                        .segmentType(SegmentType.TABLE)
                        .level(0)
                        .text(tableText.toString().trim())
                        .build();
                segments.add(seg);
            } else if (node instanceof BlockQuote blockQuote) {
                String text = extractText(blockQuote);
                if (!text.isBlank()) {
                    TextSegment seg = TextSegment.builder()
                            .segmentType(SegmentType.PARAGRAPH)
                            .level(0)
                            .text(text.trim())
                            .build();
                    segments.add(seg);
                }
            } else if (node.hasChildren()) {
                extractSegments(node.getFirstChild(), segments);
            }
            node = node.getNext();
        }
    }

    private void extractListItems(Node listNode, List<TextSegment> segments) {
        Node item = listNode.getFirstChild();
        while (item != null) {
            if (item instanceof BulletListItem || item instanceof OrderedListItem) {
                String text = extractText(item);
                if (!text.isBlank()) {
                    TextSegment seg = TextSegment.builder()
                            .segmentType(SegmentType.LIST)
                            .level(0)
                            .text(text.trim())
                            .build();
                    segments.add(seg);
                }
            }
            item = item.getNext();
        }
    }

    private String extractText(Node node) {
        StringBuilder sb = new StringBuilder();
        Node child = node.getFirstChild();
        while (child != null) {
            if (child instanceof Text text) {
                sb.append(text.getChars());
            } else if (child instanceof Code code) {
                sb.append("`").append(code.getText()).append("`");
            } else if (child instanceof Emphasis emphasis) {
                sb.append("*").append(extractText(emphasis)).append("*");
            } else if (child instanceof StrongEmphasis strong) {
                sb.append("**").append(extractText(strong)).append("**");
            } else if (child instanceof Link link) {
                sb.append("[").append(link.getText()).append("](").append(link.getUrl()).append(")");
            } else if (child instanceof Image image) {
                sb.append("![").append(image.getText()).append("](").append(image.getUrl()).append(")");
            } else if (child instanceof HtmlInline html) {
                sb.append(html.getChars());
            } else if (child instanceof HtmlEntity entity) {
                sb.append(entity.getChars());
            } else if (child instanceof SoftLineBreak) {
                sb.append("\n");
            } else if (child instanceof HardLineBreak) {
                sb.append("\n");
            } else if (child.hasChildren()) {
                sb.append(extractText(child));
            }
            child = child.getNext();
        }
        return sb.toString();
    }
}
