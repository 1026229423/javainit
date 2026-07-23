package com.orientsec.idap.core.base;


import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;

//@EnableCaching // 开启缓存
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class, MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class, WebFluxAutoConfiguration.class })
@ComponentScan(excludeFilters=@Filter(type=FilterType.ASSIGNABLE_TYPE,classes= IdapTestServer.class))
@ComponentScan(basePackageClasses = com.orientsec.idap.core.PackageBase.class)
@ComponentScan(basePackageClasses = com.orientsec.genesis.auth.PackageBase.class)
@EnableDubbo
public class IdapTestServer {
    public static void main(String[] args) {
        SpringApplication.run(IdapTestServer.class, args);
    }
}
