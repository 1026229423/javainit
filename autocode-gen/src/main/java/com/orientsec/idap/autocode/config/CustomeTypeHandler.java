package com.orientsec.idap.autocode.config;

import com.baomidou.mybatisplus.generator.config.GlobalConfig;
import com.baomidou.mybatisplus.generator.config.po.TableField;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import com.baomidou.mybatisplus.generator.config.rules.IColumnType;
import com.baomidou.mybatisplus.generator.type.ITypeConvertHandler;
import com.baomidou.mybatisplus.generator.type.TypeRegistry;

public class CustomeTypeHandler implements ITypeConvertHandler {
    @Override
    public IColumnType convert(GlobalConfig globalConfig, TypeRegistry typeRegistry, TableField.MetaInfo metaInfo) {
        String typeName = metaInfo.getTypeName().toLowerCase();
        if (typeName.contains("date") || typeName.contains("time")) {
            return DbColumnType.DATE;
        }
        return typeRegistry.getColumnType(metaInfo);
    }
}
