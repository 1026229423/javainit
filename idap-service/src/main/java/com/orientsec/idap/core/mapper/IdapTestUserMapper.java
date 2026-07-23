package com.orientsec.idap.core.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.orientsec.idap.core.model.IdapTestUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * <p>
 *  Mapper 接口
 * </p>
 */
public interface IdapTestUserMapper extends BaseMapper<IdapTestUser> {

    @Select("select * from idap_test_user ${ew.customSqlSegment}")
    List<Map<String,Object>> selectListToMap(@Param("ew") Wrapper<IdapTestUser> wrapper);
}
