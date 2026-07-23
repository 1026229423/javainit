package com.orientsec.idap.core.controller;

import com.orientsec.genesis.auth.service.GenesisUserService;
import com.orientsec.genesis.ldap.service.common.model.Employee;
import com.orientsec.idap.common.model.Result;
import com.orientsec.idap.common.model.ResultGenerator;
import com.orientsec.idap.core.service.IdapTestUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author autocode
 * @since 2026-04-09
 */
@RestController
@RequestMapping("/idap/v1/idapTestUser")
public class IdapTestUserController {
    @Autowired
    private IdapTestUserService idapTestUserService;
    @Autowired
    GenesisUserService genesisUserService;

    @GetMapping("/test")
    public Result test(){
        List<Employee> xuke = genesisUserService.getUser("许可");
        System.out.println(xuke);
        return ResultGenerator.genSuccessResult("done");
    }
}
