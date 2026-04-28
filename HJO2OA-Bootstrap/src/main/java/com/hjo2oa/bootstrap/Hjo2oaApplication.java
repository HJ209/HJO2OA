package com.hjo2oa.bootstrap;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(
        basePackages = "com.hjo2oa",
        excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = Mapper.class)
)
public class Hjo2oaApplication {

    public static void main(String[] args) {
        SpringApplication.run(Hjo2oaApplication.class, args);
    }
}
