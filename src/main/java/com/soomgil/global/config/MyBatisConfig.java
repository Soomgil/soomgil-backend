package com.soomgil.global.config;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis mapper scan 설정.
 *
 * <p>{@code com.soomgil} 하위의 {@link Mapper} interface를 찾아 XML mapper와 연결한다.
 * JPA를 사용하지 않는 persistence 정책의 기본 설정이다.
 */
@Configuration
@MapperScan(basePackages = "com.soomgil", annotationClass = Mapper.class)
public class MyBatisConfig {
}
