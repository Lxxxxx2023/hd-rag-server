package com.hd.rag.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean("pgDataSourceProperties")
    @ConfigurationProperties(prefix = "spring.datasource.postgres")
    public DataSourceProperties pgDataSourceProperties() {
        return new DataSourceProperties();
    }


    @Bean("pgDataSource")
    public DataSource pgDataSource(@Qualifier("pgDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }
}
