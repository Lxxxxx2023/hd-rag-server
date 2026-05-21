package com.hd.rag.domain.index.model.entity;

import com.hd.rag.domain.index.model.valObj.DataSourceType;
import io.micrometer.common.lang.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Connector 的产出物，交给 IParserStrategy 继续处理。
 * Connector 的唯一职责是"拉取原始字节"，文本提取和结构化解析由 Parser 完成。
 * mimeType 是 Connector 的初步判断（来自文件扩展名 / Content-Type 头），
 * ParserRegistry 以它为线索选择解析器，结合 probe(rawContent) 做最终判定。
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RawSourceDocumentEntity{
    private String externalId;
    private String externalVersion;
    /**
     * 数据源类型
     */
    private DataSourceType sourceType;
    /**
     * Connector 初步判断，可能不准（如扩展名与实际内容不符）
     */
    @Nullable
    private String mimeType;
    /**
     * 原始字节，始终有值 — 即使是 URL/API，Connector 也原样返回响应体字节
     */
    @Nullable
    private byte[] rawContent;
    /**
     * 来源特有元数据（飞书的 pageToken、URL 的 httpStatus 等）
     */
    private Map<String, Object> metadata;
}