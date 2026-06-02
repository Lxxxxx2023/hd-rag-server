package com.hd.rag.domain.index.parse;

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

/**
 * markdown 解析器
 */
@Component
public class MarkdownParser implements DocumentParser {
    @Override
    public ParserEnum getType() {
        return ParserEnum.MARKDOWN;
    }

    @Override
    public boolean canHandle(String mimeType) {
        return "text/markdown".equalsIgnoreCase(mimeType)
                || "application/markdown".equalsIgnoreCase(mimeType);
    }

    @Override
    public List<TextSegment> parse(byte[] content) {
        String rawText = new String(content, StandardCharsets.UTF_8);

        Parser parser = Parser.builder()
                .extensions(List.of(TablesExtension.create()))
                .build();

        List<TextSegment> textSegmentEntities = new ArrayList<>();
        Document document = parser.parse(rawText);

        int level = 0;
        for (Node node : document.getChildren()) {
            TextSegment textSegment = convert(node, level);
            if (textSegment != null) {
                textSegmentEntities.add(textSegment);
                level= textSegment.getLevel();
            }
        }

        return textSegmentEntities;
    }

    private TextSegment convert(Node node, int level) {
        if (node instanceof Heading h) {
            level = h.getLevel();
            return TextSegment.builder()
                    .segmentType(SegmentType.HEADING)
                    .text(cleanHeading(h))
                    .level(level)
                    .sourcePointer(lineNumber(node))
                    .build();
        }
        if (node instanceof FencedCodeBlock code) {
            return TextSegment.builder()
                    .segmentType(SegmentType.CODE)
                    .text(cleanFencedCode(code))
                    .level(level)
                    .sourcePointer(lineNumber(node))
                    .build();
        }
        if (node instanceof IndentedCodeBlock code) {
            return TextSegment.builder()
                    .segmentType(SegmentType.CODE)
                    .text(cleanIndentedCode(code))
                    .level(level)
                    .sourcePointer(lineNumber(node))
                    .build();
        }
        if (node instanceof BulletList list) {
            return TextSegment.builder()
                    .segmentType(SegmentType.LIST)
                    .text(cleanBulletList(list))
                    .level(level)
                    .sourcePointer(lineNumber(node))
                    .build();
        }
        if (node instanceof OrderedList list) {
            return TextSegment.builder()
                    .segmentType(SegmentType.LIST)
                    .text(cleanOrderedList(list))
                    .level(level)
                    .sourcePointer(lineNumber(node))
                    .build();
        }
        if (node instanceof TableBlock table) {
            return TextSegment.builder()
                    .segmentType(SegmentType.TABLE)
                    .text(cleanTable(table))
                    .level(level)
                    .sourcePointer(lineNumber(node))
                    .build();
        }
        if (node instanceof BlockQuote bq) {
            return TextSegment.builder()
                    .segmentType(SegmentType.PARAGRAPH)
                    .text(cleanBlockQuote(bq))
                    .level(level)
                    .sourcePointer(lineNumber(node))
                    .build();
        }
        if (node instanceof Paragraph p) {
            return TextSegment.builder()
                    .segmentType(SegmentType.PARAGRAPH)
                    .text(cleanInline(p))
                    .level(level)
                    .sourcePointer(lineNumber(node))
                    .build();
        }
        return null;
    }

    // ======================== cleaning helpers ========================

    /**
     * 去掉 heading 的 # 前缀，只返回标题文本
     */
    private String cleanHeading(Heading h) {
        return h.getText().toString().strip();
    }

    /**
     * 去掉围栏代码块的 ``` 标记，只返回代码内容
     */
    private String cleanFencedCode(FencedCodeBlock code) {
        return code.getContentChars().toString().strip();
    }

    /**
     * 去掉缩进代码块的缩进，只返回代码内容
     */
    private String cleanIndentedCode(IndentedCodeBlock code) {
        return code.getContentChars().toString().strip();
    }

    /**
     * 将表格转为 "列名: 值; 列名: 值" 的语义文本，每行一条
     */
    private String cleanTable(TableBlock table) {
        List<String> headers = new ArrayList<>();
        List<List<String>> rows = new ArrayList<>();

        for (Node child : table.getChildren()) {
            if (child instanceof TableHead head) {
                headers = extractRow(head);
            } else if (child instanceof TableBody body) {
                for (Node rowNode : body.getChildren()) {
                    if (rowNode instanceof TableRow) {
                        rows.add(extractRow(rowNode));
                    }
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        for (List<String> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                if (i < headers.size() && !headers.get(i).isEmpty()) {
                    sb.append(headers.get(i)).append(": ");
                }
                sb.append(row.get(i));
                if (i < row.size() - 1) {
                    sb.append("; ");
                }
            }
            sb.append("\n");
        }
        return sb.toString().strip();
    }

    private List<String> extractRow(Node rowNode) {
        List<String> cells = new ArrayList<>();
        for (Node cell : rowNode.getChildren()) {
            if (cell instanceof TableCell tc) {
                cells.add(cleanInline(tc).strip());
            }
        }
        return cells;
    }

    /**
     * 去掉无序列表的 -/* 标记，每项一行
     */
    private String cleanBulletList(BulletList list) {
        StringBuilder sb = new StringBuilder();
        for (Node child : list.getChildren()) {
            if (child instanceof BulletListItem) {
                String text = extractItemText(child);
                if (!text.isEmpty()) {
                    sb.append("- ").append(text).append("\n");
                }
            }
        }
        return sb.toString().strip();
    }

    /**
     * 去掉有序列表的数字标记，每项一行（重新编号）
     */
    private String cleanOrderedList(OrderedList list) {
        StringBuilder sb = new StringBuilder();
        int index = 1;
        for (Node child : list.getChildren()) {
            if (child instanceof OrderedListItem) {
                String text = extractItemText(child);
                if (!text.isEmpty()) {
                    sb.append(index).append(". ").append(text).append("\n");
                    index++;
                }
            }
        }
        return sb.toString().strip();
    }

    /**
     * 提取列表项的内容文本（绕过项目符号/序号标记）
     */
    private String extractItemText(Node item) {
        StringBuilder sb = new StringBuilder();
        for (Node child : item.getChildren()) {
            if (child instanceof Paragraph p) {
                String cleaned = cleanInline(p);
                if (!cleaned.isEmpty()) {
                    if (sb.length() > 0) {
                        sb.append(' ');
                    }
                    sb.append(cleaned);
                }
            }
        }
        return sb.toString().strip();
    }

    /**
     * 提取引用块内的段落文本
     */
    private String cleanBlockQuote(BlockQuote bq) {
        StringBuilder sb = new StringBuilder();
        for (Node child : bq.getChildren()) {
            if (child instanceof Paragraph p) {
                String cleaned = cleanInline(p);
                if (!cleaned.isEmpty()) {
                    if (sb.length() > 0) {
                        sb.append('\n');
                    }
                    sb.append(cleaned);
                }
            }
        }
        return sb.toString().strip();
    }

    /**
     * 递归清洗行内格式：去掉 **加粗**、*斜体*、`代码`、[链接](url)、![图片](url) 等标记，只保留文本内容
     */
    private String cleanInline(Node parent) {
        StringBuilder sb = new StringBuilder();
        for (Node child : parent.getChildren()) {
            if (child instanceof Text t) {
                sb.append(t.getChars());
            } else if (child instanceof Code c) {
                sb.append(c.getText());
            } else if (child instanceof Emphasis || child instanceof StrongEmphasis) {
                sb.append(cleanInline(child));
            } else if (child instanceof Link l) {
                sb.append(cleanInline(l));
            } else if (child instanceof Image img) {
                sb.append(cleanInline(img));
            } else if (child instanceof SoftLineBreak) {
                sb.append(' ');
            } else if (child instanceof HardLineBreak) {
                sb.append('\n');
            } else if (child instanceof HtmlEntity) {
                sb.append(child.getChars());
            } else if (child instanceof HtmlInline) {
                // 跳过原始 HTML 标签——对 embedding 是噪声
            } else {
                sb.append(cleanInline(child));
            }
        }
        return sb.toString().strip();
    }

    private String lineNumber(Node node) {
        return String.format("%s-%s", node.getStartLineNumber() + 1, node.getEndLineNumber() + 1);
    }
}
