package com.hd.rag.trigger.http;

import com.hd.rag.api.KnowledgeServer;
import com.hd.rag.api.dto.KnowledgeCreateReqDTO;
import com.hd.rag.api.dto.KnowledgeDatasourceConfigCreateReqDTO;
import com.hd.rag.api.dto.KnowledgeLoadDataReqDTO;
import com.hd.rag.domain.index.service.IDataLoadService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/knowledge")
public class KnowledgeController implements KnowledgeServer {

    @Resource
    private IDataLoadService dataLoadService;

    @Override
    public void createKnowledge(KnowledgeCreateReqDTO reqDTO) {

    }

    @Override
    @PostMapping("/loadDataByFile")
    public void loadDataByFile(MultipartFile multipartFile, String kbId) {
        dataLoadService.loadDataByFile(multipartFile, kbId);
    }

    @Override
    public void createKnowledgeDatasourceConfig(KnowledgeDatasourceConfigCreateReqDTO reqDTO) {

    }

    @Override
    public void loadData(KnowledgeLoadDataReqDTO reqDTO) {
        dataLoadService.loadData(reqDTO);
    }
}
