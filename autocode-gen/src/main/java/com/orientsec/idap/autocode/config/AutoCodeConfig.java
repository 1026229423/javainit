package com.orientsec.idap.autocode.config;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
@Data
public class AutoCodeConfig implements Serializable, InitializingBean {

    public static String rootPath = System.getProperty("user.dir");
    public static String src_java_pach = "src/main/java";
    public static String src_resource_path = "src/main/resources";

    @Value("${spring.datasource.type:com.alibaba.druid.pool.DruidDataSource}")
    public String jdbcType;
    @Value("${spring.datasource.driverClassName}")
    public String driverClassName;
    @Value("${spring.datasource.url}")
    public String jdbcUrl;
    @Value("${spring.datasource.username}")
    public String jdbcUser;
    @Value("${spring.datasource.password}")
    public String jdbcPasswd;

    @Value("${autocode.tbname.schema}")
    public String schema;
    // 是否在vo生成schema.tableName，主库建议默认不要生成
    public boolean enableSchema = false;

    @Value("${autocode.base.package}")
    public String basePackage = null;

    @Value("${autocode.proj.module}")
    public String serviceProj = null;

    public String serviceImplProj;
    public String controllerProj;
    public String mapperProj;
    public String entityProj;

    public String servicePackage;
    public String serviceImplPackage;
    public String controllerPackage;
    public String mapperPackage;
    public String extMapperPackage;
    public String entityPackage;
    public String xmlPackage;
    public String extXmlPackage;
    public boolean enableService = true;
    @Value("${autocode.controller.enable}")
    public boolean enableController = false;
    @Value("${autocode.controller.restbase}")
    public String restBase = null;
    public boolean enableXmlMapperFile = false;

    @Override
    public void afterPropertiesSet() {
        serviceImplProj = serviceProj;
        controllerProj = serviceProj;
        mapperProj = serviceProj;
        entityProj = serviceProj;

        this.servicePackage = String.format("%s.%s", basePackage, "service");
        this.serviceImplPackage = String.format("%s.%s", this.servicePackage, "impl");
        this.controllerPackage = String.format("%s.%s", basePackage, "controller");
        this.mapperPackage = String.format("%s.%s", basePackage, "mapper");
        this.entityPackage = String.format("%s.%s", basePackage, "model");
        this.xmlPackage = String.format("%s.%s", basePackage, "mapper");
        this.extMapperPackage = String.format("%s.ext", xmlPackage);
        this.extXmlPackage = String.format("%s.ext", xmlPackage);
    }

    static String comma = ".";
    static String slash = "/";
    public String formatPath(String proj, String srcPath, String modulePath) {
        modulePath = commaToSlash(modulePath);
        String ret = StrUtil.join(slash, rootPath, proj, srcPath, modulePath);
        return ret;
    }

    private static String commaToSlash(String src) {
        return StrUtil.replace(src, comma, slash);
    }

    public String getServicePath() {
        return formatPath(serviceProj, src_java_pach, this.servicePackage);
    }

    public String getServiceImplPath() {
        return formatPath(serviceImplProj, src_java_pach, this.serviceImplPackage);
    }

    public String getControllerPath() {
        return formatPath(controllerProj, src_java_pach, this.controllerPackage);
    }

    public String getMapperPath() {
        return formatPath(mapperProj, src_java_pach, this.mapperPackage);
    }

    public String getExtMapperPath() {
        return formatPath(mapperProj, src_java_pach, this.extMapperPackage);
    }

    public String getEntityPath() {
        return formatPath(entityProj, src_java_pach, this.entityPackage);
    }

    public String getXmlPath() {
        return formatPath(entityProj, src_resource_path, this.xmlPackage);
    }

    public String getExtXmlPath() {
        return formatPath(entityProj, src_resource_path, this.extXmlPackage);
    }

    public String getExtMapperParentPkg() {
        return mapperPackage;
    }
}
