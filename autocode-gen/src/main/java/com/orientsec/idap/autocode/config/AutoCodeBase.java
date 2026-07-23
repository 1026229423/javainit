package com.orientsec.idap.autocode.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import java.util.List;

@ComponentScan("com.orientsec.idap.autocode")
@SpringBootApplication
public class AutoCodeBase {
    public static void doStart(String env, List<String> tables, String[] args) {
        System.setProperty("spring.profiles.active", env);
        ConfigurableApplicationContext context = SpringApplication.run(AutoCodeBase.class, args);
        AutoCodeConfig props = context.getBean(AutoCodeConfig.class);
        CodeGenerator.Run(props, tables);
        context.close();
    }
}
