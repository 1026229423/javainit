package com.orientsec.idap.core.config;

import com.orientsec.idap.common.config.DataSourceConfigBase;
import com.orientsec.idap.core.mapper.ForPackage;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

@Configuration
public class IdapDataSourceConfig extends DataSourceConfigBase {

    @Bean(name = ConfigConstant.Data_Source_NAME)
    @ConfigurationProperties(prefix = ConfigConstant.Data_Source_ConfigPrefix)
    public DataSource primaryDataSource(){ return new HikariDataSource();//DataSourceBuilder.create().build();
    }


    @Bean(name = ConfigConstant.JdbcTemplate)
    public JdbcTemplate jdbcTemplate(@Qualifier(ConfigConstant.Data_Source_NAME) DataSource dataSource) throws Exception {
        JdbcTemplate template = new JdbcTemplate(dataSource);
        template.setFetchSize(Integer.MIN_VALUE);
//      template.setMaxRows(200);
        template.afterPropertiesSet();
        return template;
    }

    @Bean(name = ConfigConstant.SqlSession_Factory_NAME)
    public SqlSessionFactory SqlSessionFactory(@Qualifier(ConfigConstant.Data_Source_NAME) DataSource dataSource,
                                               @Value(ConfigConstant.Data_Source_MapperLoc) String mapperLocation) throws Exception {
        return buildSqlSessionFactory(dataSource, mapperLocation);
    }

    @Bean(name = ConfigConstant.Transaction_Manager)
    public DataSourceTransactionManager TransactionManager(@Qualifier(ConfigConstant.Data_Source_NAME) DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean(name = ConfigConstant.SqlSession_Template)
    public SqlSessionTemplate SqlSessionTemplate(@Qualifier(ConfigConstant.SqlSession_Factory_NAME) SqlSessionFactory sqlSessionFactory) throws Exception {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean(name = ConfigConstant.MapperScannerConfigurer)
    public MapperScannerConfigurer MapperScannerConfigurer() {
        return buildMapperScannerConfigurer(ConfigConstant.SqlSession_Factory_NAME,
                ForPackage.class.getPackage().getName());
    }
}
