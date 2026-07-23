package com.orientsec.genesis.auth.service;

import com.orientsec.genesis.auth.common.model.Menu;
import com.orientsec.genesis.auth.common.model.Role;
import com.orientsec.genesis.auth.common.model.User;
import com.orientsec.genesis.ldap.service.common.model.Department;
import com.orientsec.genesis.ldap.service.common.model.Employee;

import java.util.List;

public interface GenesisUserService {
    User getProjectUserByName(String userId);

    public String getUserCNName(String userId);

    public User getCurrentUser();

    public Employee getCurrentEmployee();

    public List<Menu> getUserMenus();

    public void logout();

    public List<Employee> getUser(String name) ;

    public List<Department> getDepartment(int type) ;

    Employee getEmployeeByName(String userName);

    List<Role> getUserRoles(Long var1);

    String getProjectKey();
}
