package com.orientsec.genesis.auth.service.impl;

import com.orientsec.genesis.auth.common.model.Menu;
import com.orientsec.genesis.auth.common.model.Role;
import com.orientsec.genesis.auth.common.model.User;
import com.orientsec.genesis.auth.dubbo.api.UserDubboService;
import com.orientsec.genesis.auth.service.UserDubboServiceProxy;
import com.orientsec.genesis.auth.service.GenesisUserService;
import com.orientsec.genesis.ldap.dubbo.api.LDAPDubboService;
import com.orientsec.genesis.ldap.service.common.model.Department;
import com.orientsec.genesis.ldap.service.common.model.Employee;
import com.orientsec.idap.common.utils.LogHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class GenesisUserServiceImpl implements GenesisUserService {

    @DubboReference
    private UserDubboService userDubboService;

    @DubboReference
    private LDAPDubboService ldapDubboService;

    @Autowired
    private UserDubboServiceProxy userDubboServiceProxy;

    @Value(value = "${proj.genesis-key:ibond}")
    private String proj_key;

    public User getProjectUserByName(String userId){
        log.info("getProjectUserByName {} {}",proj_key,userId);
        User user = userDubboService.getProjectUserByName(proj_key,userId );
        return user;
    }

    public String getUserCNName(String userId){
        String userName = "";
        User user = getProjectUserByName(userId);
        if(user !=null){
            userName = user.getChineseName();
        }
        LogHelper.log(log,"getUserCNName",userId,userName);
        return userName;
    }


    @Override
    public User getCurrentUser() {
        String userGri = (String)SecurityUtils.getSubject().getPrincipal();
        log.info("get userGri getPrincipal {} {}",proj_key,userGri);
        User user = userDubboServiceProxy.getUser(userGri);
        if(user == null) {
            throw new RuntimeException("用户未在项目中配置权限！");
        }
        log.info("getCurrentUser {}",user.getChineseName());
        return user;
    }

    @Override
    public List<Menu> getUserMenus() {
        User user = this.getCurrentUser();
        List<Menu> list = userDubboService.getAuthorizedMenuByUser(user);
        return list;
    }

    @Override
    public Employee getCurrentEmployee() {
        User user = this.getCurrentUser();
        Employee employee = userDubboServiceProxy.getEmployeeByName(user.getName());
        String departmentId = employee.getDepartmentId();
        Department department = ldapDubboService.getDepartmentById(departmentId);
        String departmentName = department == null ? departmentId: department.getName();
        employee.set("departmentName", departmentName);
        return employee;
    }

    @Override
    public void logout() {
        if(SecurityUtils.getSubject().isAuthenticated()) {
            SecurityUtils.getSubject().logout();
        }
    }

    @Override
    public List<Employee> getUser(String name) { return ldapDubboService.searchEmployeeByKey(name); }

    @Override
    public List<Department> getDepartment( int type) { return userDubboServiceProxy.getAllDeptId( type); }

    public Department getDepartmentById(String departmentId) {
        Department department = ldapDubboService.getDepartmentById(departmentId);
        return department;
    }

    public Employee getEmployeeByName(String userName){
        Employee employee = ldapDubboService.getEmployeeByName(userName);
        if(employee == null){
            Employee emp = new Employee();
            emp.setId(userName);
            emp.setName(userName);
            emp.set("departmentName", StringUtils.EMPTY);
            return emp;
        }
        String departmentId = employee.getDepartmentId();
        Department department = getDepartmentById(departmentId);
        String departmentName = department == null ? StringUtils.EMPTY: department.getName();
        employee.set("departmentName", departmentName);
        return employee;
    }

    public List<Role> getUserRoles(Long var1){ return userDubboService.getUserRoles(var1); }

    @Override
    public String getProjectKey(){ return proj_key; }
}
