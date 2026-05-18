package com.hd.rag.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.config.EnableElasticsearchAuditing;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.hd.rag.infrastructure.dao")
@EnableElasticsearchAuditing
public class ElasticsearchConfig {
}
