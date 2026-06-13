package com.hd.rag.trigger.http.ingestion;

import com.hd.rag.api.ingestion.dto.request.KnowledgeBaseCreateReqDTO;
import com.hd.rag.api.ingestion.dto.request.KnowledgeBaseUpdateReqDTO;
import com.hd.rag.api.ingestion.dto.response.KnowledgeBaseRespDTO;
import com.hd.rag.domain.ingestion.service.IKnowledgeBaseService;
import com.hd.rag.types.convention.Result;
import com.hd.rag.types.web.Results;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 知识库controller
 */
@RestController
@RequestMapping("/knowledgebase")
public class KnowledgeBaseController {

    @Resource
    private IKnowledgeBaseService knowledgeBaseService;

    @PostMapping("/create")
    public Result<KnowledgeBaseRespDTO> create(@RequestBody @Valid KnowledgeBaseCreateReqDTO reqDTO) {
        return Results.success(knowledgeBaseService.create(reqDTO));
    }

    @PutMapping("/update")
    public Result<KnowledgeBaseRespDTO> update(@RequestBody @Valid KnowledgeBaseUpdateReqDTO reqDTO) {
        return Results.success(knowledgeBaseService.update(reqDTO));
    }

    @GetMapping("/get/{id}")
    public Result<KnowledgeBaseRespDTO> getById(@PathVariable String id) {
        return Results.success(knowledgeBaseService.getById(id));
    }

    @GetMapping("/list")
    public Result<List<KnowledgeBaseRespDTO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Results.success(knowledgeBaseService.list(pageNum, pageSize));
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable String id) {
        knowledgeBaseService.delete(id);
        return Results.success();
    }
}
