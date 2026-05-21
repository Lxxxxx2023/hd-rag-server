package com.hd.rag.domain.index.service.chain;

import com.hd.rag.domain.index.service.chain.node.RootNode;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;


/**
 * 数据处理工厂 目前只处理单个文件
 */
@Service
public class DefaultDataHandleFactory {

    @Resource
    private RootNode rootNode;

    public void handleData(DataHandleContent dataHandleContent) throws Exception {
        rootNode.handle(dataHandleContent);
    }
}
