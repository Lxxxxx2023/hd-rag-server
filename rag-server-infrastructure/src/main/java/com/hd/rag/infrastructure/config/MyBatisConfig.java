package com.hd.rag.infrastructure.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.hd.rag.infrastructure.dao")
public class MyBatisConfig {
}
