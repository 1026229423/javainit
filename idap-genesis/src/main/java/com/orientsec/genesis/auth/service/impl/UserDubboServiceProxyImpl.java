package com.orientsec.genesis.auth.service.impl;
import com.orientsec.genesis.auth.common.model.User;
import com.orientsec.genesis.auth.dubbo.api.UserDubboService;
import com.orientsec.genesis.auth.service.UserDubboServiceProxy;
import com.orientsec.genesis.ldap.dubbo.api.LDAPDubboService;
import com.orientsec.genesis.ldap.service.common.model.Department;
import com.orientsec.genesis.ldap.service.common.model.Employee;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class UserDubboServiceProxyImpl implements UserDubboServiceProxy {
    @DubboReference
    private UserDubboService userDubboService;

    @DubboReference
    private LDAPDubboService ldapDubboService;

    @Override
//  @Cacheable(key = Constants.CACHE_KEY_SINGLE_PARAM)
    public User getUser(String userGri) {
        log.info("getUser by {}",userGri);
        userGri = parseGri(userGri);
        if(StringUtils.isBlank(userGri)) {
            throw new RuntimeException("未知用户登录信息！");
        }
        return userDubboService.getUser(userGri);
    }

    @Override
//  @Cacheable(key = Constants.CACHE_KEY_SINGLE_PARAM)
    public Employee getEmployeeByName(String name) {
        log.info("getEmployeeByName by {}",name);
        return ldapDubboService.getEmployeeByName(name);
    }

    private String parseGri(String userGri) {
        if(StringUtils.isNotBlank(userGri)) {
            int first = userGri.indexOf(":");
            return userGri.substring(first+1);
        }
        return null;
    }

    @Override
//  @Cacheable(key = Constants.CACHE_KEY_ALL_DEPT)
    public List<Department> getAllDeptId(int type) {
        List<Department> ret = new ArrayList<Department>();
        List<Department> dept = ldapDubboService.getAllDepartment();
        for (Department department : dept) {
            if(department.getIsVirtual() == null || department.getIsVirtual()) {
                continue;
            }
            if(department.getInstitutionType() ==null) {
                continue;
            }
            if(department.getInstitutionType() ==type) {
                ret.add(department);
            }
        }
        return ret;
    }
}
