package com.hd.rag.trigger.http.ingestion;

import com.hd.rag.api.ingestion.dto.request.KnowledgeBaseCreateReqDTO;
import com.hd.rag.api.ingestion.dto.request.KnowledgeBaseUpdateReqDTO;
import com.hd.rag.api.ingestion.dto.response.KnowledgeBaseRespDTO;
import com.hd.rag.domain.ingestion.model.entity.KnowledgeBaseEntity;
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
import java.util.stream.Collectors;

/**
 * 知识库接口
 */
@RestController
@RequestMapping("/knowledgebase")
public class KnowledgeBaseController {

    @Resource
    private IKnowledgeBaseService knowledgeBaseService;

    @PostMapping("/create")
    public Result<KnowledgeBaseRespDTO> create(@RequestBody @Valid KnowledgeBaseCreateReqDTO reqDTO) {
        KnowledgeBaseEntity knowledgeBaseEntity = new KnowledgeBaseEntity();
        knowledgeBaseEntity.setName(reqDTO.name());
        knowledgeBaseEntity.setIntro(reqDTO.intro());
        knowledgeBaseEntity.setSearchSet(reqDTO.searchSet());
        knowledgeBaseEntity.setSegmentSet(reqDTO.segmentSet());
        knowledgeBaseEntity.setCreateBy("SYSTEM");
        knowledgeBaseEntity.setUpdateBy("SYSTEM");
        KnowledgeBaseEntity created = knowledgeBaseService.create(knowledgeBaseEntity);
        return Results.success(toRespDTO(created));
    }

    @PutMapping("/update")
    public Result<KnowledgeBaseRespDTO> update(@RequestBody @Valid KnowledgeBaseUpdateReqDTO reqDTO) {
        KnowledgeBaseEntity knowledgeBaseEntity = new KnowledgeBaseEntity();
        knowledgeBaseEntity.setId(reqDTO.id());
        knowledgeBaseEntity.setName(reqDTO.name());
        knowledgeBaseEntity.setIntro(reqDTO.intro());
        knowledgeBaseEntity.setSearchSet(reqDTO.searchSet());
        knowledgeBaseEntity.setUpdateBy("SYSTEM");
        KnowledgeBaseEntity updated = knowledgeBaseService.update(knowledgeBaseEntity);
        return Results.success(toRespDTO(updated));
    }

    @GetMapping("/get/{id}")
    public Result<KnowledgeBaseRespDTO> getById(@PathVariable String id) {
        KnowledgeBaseEntity knowledgeBaseEntity = knowledgeBaseService.getById(id);
        return Results.success(toRespDTO(knowledgeBaseEntity));
    }

    @GetMapping("/list")
    public Result<List<KnowledgeBaseRespDTO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        List<KnowledgeBaseEntity> list = knowledgeBaseService.list(new KnowledgeBaseEntity(), pageNum, pageSize);
        List<KnowledgeBaseRespDTO> respList = list.stream()
                .map(this::toRespDTO)
                .collect(Collectors.toList());
        return Results.success(respList);
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable String id) {
        knowledgeBaseService.delete(id);
        return Results.success();
    }

    private KnowledgeBaseRespDTO toRespDTO(KnowledgeBaseEntity kb) {
        return new KnowledgeBaseRespDTO(
                kb.getId(),
                kb.getName(),
                kb.getIntro(),
                kb.getSearchSet(),
                kb.getSegmentSet(),
                kb.getCreateBy(),
                kb.getCreateTime(),
                kb.getUpdateBy(),
                kb.getUpdateTime());
    }
}
