package com.hd.rag.domain.index.service.connector;

import com.hd.rag.domain.index.model.entity.DataSourceEntity;
import com.hd.rag.domain.index.model.entity.RawSourceDocumentEntity;
import com.hd.rag.domain.index.model.valObj.ConnectionTestResult;
import com.hd.rag.domain.index.model.valObj.DataSourceType;
import com.hd.rag.domain.index.model.valObj.WebhookPayload;

import java.time.Instant;
import java.util.List;

public abstract class AbstractDefaultConnector implements ISourceConnector {
    @Override
    public abstract DataSourceType getType();

    @Override
    public abstract List<RawSourceDocumentEntity> fetchAll(DataSourceEntity source);

    @Override
    public abstract ConnectionTestResult testConnection(DataSourceEntity config);

    @Override
    public List<RawSourceDocumentEntity> fetchUpdated(DataSourceEntity source, Instant since) {
        return List.of();
    }

    @Override
    public List<RawSourceDocumentEntity> handleWebhook(DataSourceEntity source, WebhookPayload payload) {
        return List.of();
    }
}
