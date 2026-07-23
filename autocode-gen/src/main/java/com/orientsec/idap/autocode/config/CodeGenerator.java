package com.orientsec.idap.autocode.config;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.SerializationUtils;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.PackageConfig;
import com.baomidou.mybatisplus.generator.config.builder.Mapper;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 需要检查main方法的输出路径
 */
public class CodeGenerator {

    public static void Run(AutoCodeConfig config, boolean enableService, boolean enableController, String ... tableList) {
        Run(config, enableService, enableController, Arrays.asList(tableList));
    }

    public static void Run(AutoCodeConfig configOrig, boolean enableService,boolean enableController, List<String> tableList) {
        AutoCodeConfig config = SerializationUtils.clone(configOrig);
        config.enableService = enableService;
        config.enableController = enableController;
        DataSourceConfig.Builder datasourceConfig = new DataSourceConfig.Builder(config.jdbcUrl, config.jdbcUser, config.jdbcPasswd)
                .typeConvertHandler(new CustomeTypeHandler()).schema(config.schema);
        Run(datasourceConfig, tableList, config);
    }

    public static void Run(AutoCodeConfig config, List<String> tableList) {
        DataSourceConfig.Builder datasourceConfig = new DataSourceConfig.Builder(config.jdbcUrl, config.jdbcUser, config.jdbcPasswd)
                .typeConvertHandler(new CustomeTypeHandler()).schema(config.schema);
        Run(datasourceConfig, tableList, config);
    }

    public static void Run(DataSourceConfig.Builder datasourceConfig, List<String> tableList, AutoCodeConfig config) {
        Assert.notEmpty(tableList, "请传入需要生成的表列表");
        FastAutoGenerator.create(datasourceConfig)
                .globalConfig(builder -> builder.disableOpenDir().author("autocode"))
                .packageConfig(builder -> {
                    setPackageConfig(builder,config);
                    builder.pathInfo(getOutputPathInfo(config));
                })
                .injectionConfig(consumer->{
                    HashMap<String, Object> customMap = new HashMap<>();
                    customMap.put("controllerPath",config.restBase);
                    consumer.customMap(customMap);
                })
                .strategyConfig(builder -> {
                    if (CollectionUtils.isNotEmpty(tableList)) {
                        builder.addInclude(tableList);
                    }
                    if (StrUtil.isNotBlank(config.schema)) {
                        builder.enableSchema();
                    }

                    builder.entityBuilder()
                            .enableRemoveIsPrefix()
                            .enableTableFieldAnnotation()
                            .javaTemplate("/plusTemplate/entity.java.vm")
                            .enableFileOverride();

                    Mapper.Builder mapperBuilder = builder.mapperBuilder();
                    mapperBuilder.formatMapperFileName("%sMapper")
                            .mapperTemplate("/plusTemplate/mapper.java.vm");
                    if(!config.enableXmlMapperFile){
                        mapperBuilder.disableMapperXml();
                    }else {
                        mapperBuilder.enableBaseColumnList().enableBaseResultMap();
                    }
                    mapperBuilder.enableFileOverride();

                    builder.controllerBuilder()
                            .enableRestStyle()
                            .template("/plusTemplate/controller.java.vm")
                            .formatFileName("%sController");

                    builder.serviceBuilder()
                            .formatServiceFileName("%sService")
                            .formatServiceImplFileName("%sServiceImpl");

                    if(!config.enableService){
                        builder.serviceBuilder().disableService().disableServiceImpl();
                    }
                    if(!config.enableController){
                        builder.controllerBuilder().disable();
                    }
                })
                .execute();
        // mapper ext
        FastAutoGenerator.create(datasourceConfig)
                .globalConfig(builder -> builder.disableOpenDir().author("autocode"))
                .packageConfig(builder -> {
                    builder.parent(StrUtil.EMPTY)
                            .mapper(config.extMapperPackage)
                            .entity(config.entityPackage)
                            .xml(config.extXmlPackage);
                    builder.pathInfo(getExtPathInfo(config));
                })
                .injectionConfig(consumer->{
                    HashMap<String, Object> customMap = new HashMap<>();
                    customMap.put("extParentPkg",config.getExtMapperParentPkg());
                    consumer.customMap(customMap);
                })
                .strategyConfig(builder -> {
                    if (CollectionUtils.isNotEmpty(tableList)) {
                        builder.addInclude(tableList);
                    }
                    builder.entityBuilder().disable();
                    builder.serviceBuilder().disable();
                    builder.controllerBuilder().disable();
                    builder.mapperBuilder()
                            .mapperTemplate("/plusTemplate/mapper-ext.java.vm")
                            .formatMapperFileName("%sMapperExt")
                            .formatXmlFileName("%sMapperExt");
                })
                .execute();
    }

    private static void setPackageConfig(PackageConfig.Builder builder, AutoCodeConfig config) {
        builder.parent(StrUtil.EMPTY/*config.basePackage*/)
                .entity(config.entityPackage)
                .mapper(config.mapperPackage)
                .service(config.servicePackage)
                .serviceImpl(config.serviceImplPackage)
                .controller(config.controllerPackage);
    }

    private static Map<OutputFile, String> getOutputPathInfo(AutoCodeConfig config) {
        Map<OutputFile, String> pathInfo = new HashMap<>(6);
        pathInfo.put(OutputFile.entity, config.getEntityPath());
        pathInfo.put(OutputFile.mapper, config.getMapperPath());
        pathInfo.put(OutputFile.service, config.getServicePath());
        pathInfo.put(OutputFile.serviceImpl, config.getServiceImplPath());
        pathInfo.put(OutputFile.controller, config.getControllerPath());
        pathInfo.put(OutputFile.xml, config.getXmlPath());
        return pathInfo;
    }

    private static Map<OutputFile,String> getExtPathInfo(AutoCodeConfig config){
        Map<OutputFile, String> pathInfo = new HashMap<>(8);
        pathInfo.put(OutputFile.mapper,config.getExtMapperPath());
        pathInfo.put(OutputFile.xml,config.getExtXmlPath());
        return pathInfo;
    }
}
