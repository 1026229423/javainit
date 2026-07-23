package com.orientsec.idap.core.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.orientsec.idap.core.base.AbstractTestBase;
import com.orientsec.idap.core.mapper.ext.IdapTestUserMapperExt;
import com.orientsec.idap.core.model.IdapTestUser;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class IdapTestUserServiceTest extends AbstractTestBase {

    @Autowired
    IdapTestUserService idapTestUserService;

    @Autowired
    IdapTestUserMapperExt idapTestUserMapperExt;

    @Test
    public void test(){
        List<IdapTestUser> list = idapTestUserService.list();
        LambdaQueryWrapper<IdapTestUser> param = new LambdaQueryWrapper<>();
        param.eq(IdapTestUser::getId,1);
        List<IdapTestUser> list2 = idapTestUserMapperExt.selectList(param);
        System.out.println(list);
    }
}
