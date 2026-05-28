package com.hd.rag.domain.index.service.chain.node;

import com.hd.rag.domain.index.model.valObj.CanonicalDocument;
import com.hd.rag.domain.index.model.valObj.ContentNode;
import com.hd.rag.domain.index.service.chain.DataHandleContent;
import com.hd.rag.domain.index.service.chain.DataIndexResult;
import com.hd.rag.domain.index.service.chain.IDataIndexChain;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 数据清洗节点
 */
@Component
public class DataCleanNode implements IDataIndexChain {

    @Resource
    private DataChunkNode dataChunkNode;
    @Override
    public IDataIndexChain next() {
        return dataChunkNode;
    }

    /**
     * 文档 可能出现乱码、特殊字符、重复、多余空格、目录残留
     * @param dataHandleContent 数据处理上下文
     * @return 数据处理结果
     */
    @Override
    public DataIndexResult handle(DataHandleContent dataHandleContent) {
        CanonicalDocument canonicalDocument = dataHandleContent.getCanonicalDocument();

        ContentNode root = canonicalDocument.getRoot();
        clean(root);

        return dataChunkNode.handle(dataHandleContent);
    }

    private void clean(ContentNode node) {
        List<ContentNode> children = node.getChildren();
        if(CollectionUtils.isEmpty(children)) {
            return;
        }

        String plainText = node.getPlainText();
        node.setPlainText(clearGarbledText(plainText));

        for (ContentNode child : children) {
            clean(child);
        }

    }
    private String clearGarbledText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return text.replace('\uFFFD', ' ').replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", " ");
    }
}
