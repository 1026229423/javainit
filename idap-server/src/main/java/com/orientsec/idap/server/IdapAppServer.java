package com.orientsec.idap.server;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

import java.util.Date;

//@EnableCaching // 开启缓存
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class, MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class, WebFluxAutoConfiguration.class })
@ComponentScan(excludeFilters=@ComponentScan.Filter(type= FilterType.ASSIGNABLE_TYPE,classes= IdapAppServer.class))
@ComponentScan(basePackageClasses = com.orientsec.idap.core.PackageBase.class)
@ComponentScan(basePackageClasses = com.orientsec.genesis.auth.PackageBase.class)
@Slf4j
@EnableDubbo
public class IdapAppServer {
    public static void main(String[] args) throws Exception {
        SpringApplication.run(IdapAppServer.class, args);
        log.info("server start up @ {} ", new Date());
    }
}
