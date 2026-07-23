package com.orientsec.idap.core.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.orientsec.idap.core.model.IdapUserInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 用户信息表 Mapper 接口
 * </p>
 */
public interface IdapUserInfoMapper extends BaseMapper<IdapUserInfo> {

    @Select("select * from idap_user_info ${ew.customSqlSegment}")
    List<Map<String,Object>> selectListToMap(@Param("ew") Wrapper<IdapUserInfo> wrapper);
}
