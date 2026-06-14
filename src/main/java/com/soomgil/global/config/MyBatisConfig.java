package com.soomgil.global.config;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = "com.soomgil", annotationClass = Mapper.class)
public class MyBatisConfig {
}
