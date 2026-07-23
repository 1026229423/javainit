package com.orientsec.genesis.auth;

import com.orientsec.genesis.auth.service.GenesisUserService;
import com.orientsec.genesis.ldap.service.common.model.Employee;
import com.orientsec.idap.core.base.AbstractTestBase;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class UserServiceTest extends AbstractTestBase {

    @Autowired
    GenesisUserService genesisUserService;

    @Test
    public void test(){
        List<Employee> xuke = genesisUserService.getUser("许可");
        System.out.println(xuke);
    }
}
