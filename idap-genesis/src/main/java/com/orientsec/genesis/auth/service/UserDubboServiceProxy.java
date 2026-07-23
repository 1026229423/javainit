package com.orientsec.genesis.auth.service;

import com.orientsec.genesis.auth.common.model.User;
import com.orientsec.genesis.ldap.service.common.model.Department;
import com.orientsec.genesis.ldap.service.common.model.Employee;

import java.util.List;

public interface UserDubboServiceProxy {
    User getUser(String userGri);

    Employee getEmployeeByName(String name);

    public List<Department> getAllDeptId(int type);
}
