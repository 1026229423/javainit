package com.orientsec.idap.common.config;

import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.github.pagehelper.PageInterceptor;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

public abstract class DataSourceConfigBase {

    protected SqlSessionFactory buildSqlSessionFactory(DataSource datasource,String mapperLocattion) throws Exception {
        MybatisSqlSessionFactoryBean bean = new MybatisSqlSessionFactoryBean();
        bean.setDataSource(datasource);
        PageInterceptor pageHelperPlugin = new PageInterceptor();
        Interceptor[] plugins = new Interceptor[] {pageHelperPlugin};
        bean.setPlugins(plugins);
        bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources(mapperLocattion));
        return bean.getObject();
    }

    protected MapperScannerConfigurer buildMapperScannerConfigurer(String sqlSessionFactory,
                                                                    String baseBasePackage) {
        MapperScannerConfigurer mapperScannerConfigurer = new MapperScannerConfigurer();
        mapperScannerConfigurer.setSqlSessionFactoryBeanName(sqlSessionFactory);
        mapperScannerConfigurer.setBasePackage(baseBasePackage);
        return mapperScannerConfigurer;
    }
}
