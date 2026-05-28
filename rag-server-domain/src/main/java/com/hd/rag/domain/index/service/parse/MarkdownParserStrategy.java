package com.hd.rag.domain.index.service.parse;

import com.hd.rag.domain.index.model.valObj.CanonicalDocument;
import com.hd.rag.domain.index.model.valObj.ContentNode;
import com.hd.rag.domain.index.model.valObj.ContentNodeType;
import com.hd.rag.domain.index.service.chain.RawData;

import com.vladsch.flexmark.ast.BlockQuote;
import com.vladsch.flexmark.ast.BulletList;
import com.vladsch.flexmark.ast.BulletListItem;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.IndentedCodeBlock;
import com.vladsch.flexmark.ast.OrderedList;
import com.vladsch.flexmark.ast.OrderedListItem;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.ThematicBreak;
import com.vladsch.flexmark.ext.tables.TableBlock;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.BlankLine;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class MarkdownParserStrategy implements IParseStrategy {

    private static final Logger log = LoggerFactory.getLogger(MarkdownParserStrategy.class);

    @Override
    public boolean canHandle(String mimeType) {
        return "text/markdown".equalsIgnoreCase(mimeType)
                || "application/markdown".equalsIgnoreCase(mimeType);
    }

    @Override
    public CanonicalDocument parse(RawData rawData) {
        String content = new String(rawData.getContent(), StandardCharsets.UTF_8);

        Parser parser = Parser.builder()
                .extensions(List.of(TablesExtension.create()))
                .build();
        Document document = parser.parse(content);

        ContentNode root = ContentNode.builder()
                .type(ContentNodeType.PAGE)
                .plainText("")
                .markdown("")
                .build();

        for (Node node : document.getChildren()) {
            ContentNode child = convertNode(node);
            if (child != null) {
                root.getChildren().add(child);
            }
        }

        return CanonicalDocument.builder()
                .sourceType("FILE")
                .mimeType(rawData.getMimeType())
                .root(root)
                .build();
    }

    private ContentNode convertNode(Node node) {
        if (node instanceof Heading h) {
            return ContentNode.builder()
                    .type(ContentNodeType.HEADING)
                    .sourcePointer(lineNumber(node))
                    .markdown(h.getChars().toString())
                    .plainText(h.getText().toString().trim())
                    .build();
        }
        if (node instanceof FencedCodeBlock cb) {
            return ContentNode.builder()
                    .type(ContentNodeType.CODE)
                    .sourcePointer(lineNumber(node))
                    .markdown(cb.getChars().toString())
                    .plainText(cb.getContentChars().toString())
                    .build();
        }
        if (node instanceof IndentedCodeBlock cb) {
            return ContentNode.builder()
                    .type(ContentNodeType.CODE)
                    .sourcePointer(lineNumber(node))
                    .markdown(cb.getChars().toString())
                    .plainText(cb.getContentChars().toString())
                    .build();
        }
        if (node instanceof BulletList || node instanceof OrderedList) {
            ContentNode listNode = ContentNode.builder()
                    .type(ContentNodeType.LIST)
                    .sourcePointer(lineNumber(node))
                    .markdown("")
                    .plainText("")
                    .build();
            for (Node child : node.getChildren()) {
                ContentNode item = convertListItem(child);
                if (item != null) {
                    listNode.getChildren().add(item);
                }
            }
            return listNode;
        }
        if (node instanceof TableBlock) {
            return ContentNode.builder()
                    .type(ContentNodeType.TABLE)
                    .sourcePointer(lineNumber(node))
                    .markdown(node.getChars().toString())
                    .plainText(node.getChars().toString())
                    .build();
        }
        if (node instanceof BlockQuote) {
            return ContentNode.builder()
                    .type(ContentNodeType.PARAGRAPH)
                    .sourcePointer(lineNumber(node))
                    .markdown(node.getChars().toString())
                    .plainText(node.getChars().toString())
                    .build();
        }
        if (node instanceof Paragraph) {
            String text = node.getChars().toString();
            if (text.isBlank()) {
                return null;
            }
            return ContentNode.builder()
                    .type(ContentNodeType.PARAGRAPH)
                    .sourcePointer(lineNumber(node))
                    .markdown(text)
                    .plainText(node.getChars().toString())
                    .build();
        }
        if (node instanceof ThematicBreak) {
            return null;
        }
        if (node instanceof BlankLine) {
            return null;
        }
        // fallback
        String text = node.getChars().toString();
        if (text.isBlank()) {
            return null;
        }
        return ContentNode.builder()
                .type(ContentNodeType.PARAGRAPH)
                .sourcePointer(lineNumber(node))
                .markdown(text)
                .plainText(node.getChars().toString())
                .build();
    }

    private ContentNode convertListItem(Node item) {
        if (item instanceof BulletListItem || item instanceof OrderedListItem) {
            StringBuilder markdown = new StringBuilder();
            StringBuilder plainText = new StringBuilder();

            for (Node child : item.getChildren()) {
                if (child instanceof Paragraph p) {
                    if (!markdown.isEmpty()) {
                        markdown.append("\n");
                        plainText.append("\n");
                    }
                    markdown.append(p.getChars());
                    plainText.append(p.getChars());
                }
            }

            return ContentNode.builder()
                    .type(ContentNodeType.LIST)
                    .sourcePointer(lineNumber(item))
                    .markdown(markdown.toString())
                    .plainText(plainText.toString())
                    .build();
        }
        return null;
    }

    private String lineNumber(Node node) {
        int offset = node.getStartOffset();
        BasedSequence chars = node.getDocument().getChars();
        int line = 1;
        for (int i = 0; i < offset && i < chars.length(); i++) {
            if (chars.charAt(i) == '\n') {
                line++;
            }
        }
        return String.valueOf(line);
    }
}
