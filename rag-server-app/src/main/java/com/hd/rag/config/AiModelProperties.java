package com.hd.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration("aiModelProperties")
@ConfigurationProperties(prefix = "ai")
public class AiModelProperties {

    private ChatModel chat = new ChatModel();

    private EmbeddingModel embedding = new EmbeddingModel();


    @Data
    public static class ChatModel {
        private String model;
        private String baseUrl;
        private String apiKey;
        private String completionsPath;
    }
    @Data
    public static class EmbeddingModel {
        private String model;
        private String baseUrl;
        private String apiKey;
        private String embeddingsPath;
        private Integer dimensions;
    }
}
