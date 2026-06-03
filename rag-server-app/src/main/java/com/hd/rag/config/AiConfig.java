package com.hd.rag.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiModelProperties.class)
public class AiConfig {

    @Bean("openAiApi")
    public OpenAiApi openAiApi(@Qualifier("aiModelProperties") AiModelProperties properties) {
        AiModelProperties.ChatModel chat = properties.getChat();
        return OpenAiApi.builder()
                .baseUrl(chat.getBaseUrl())
                .apiKey(chat.getApiKey())
                .completionsPath(chat.getCompletionsPath())
                .build();
    }

    @Bean("openAiChatModel")
    public OpenAiChatModel openAiChatModel(@Qualifier("openAiApi") OpenAiApi openAiApi,
                                           @Qualifier("aiModelProperties") AiModelProperties properties) {
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(properties.getChat().getModel())
                        .build())
                .build();
    }

    @Bean("chatClient")
    public ChatClient chatClient(@Qualifier("openAiChatModel") OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel)
                .build();
    }

    @Bean("embeddingAiApi")
    public OpenAiApi embeddingAiApi(@Qualifier("aiModelProperties") AiModelProperties properties) {
        AiModelProperties.EmbeddingModel embedding = properties.getEmbedding();
        return OpenAiApi.builder()
                .baseUrl(embedding.getBaseUrl())
                .apiKey(embedding.getApiKey())
                .embeddingsPath(embedding.getEmbeddingsPath())
                .build();
    }

    @Bean("embeddingModel")
    public EmbeddingModel embeddingModel(@Qualifier("embeddingAiApi") OpenAiApi embeddingAiApi,
                                         @Qualifier("aiModelProperties") AiModelProperties properties) {
        AiModelProperties.EmbeddingModel embedding = properties.getEmbedding();
        return new OpenAiEmbeddingModel(embeddingAiApi, MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder()
                        .model(embedding.getModel())
                        .dimensions(embedding.getDimensions())
                        .build()
        );
    }
}
