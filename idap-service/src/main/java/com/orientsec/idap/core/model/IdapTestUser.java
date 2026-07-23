package com.orientsec.idap.core.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 *
 * </p>
 */
@TableName("idap_test_user")
public class IdapTestUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("id")
    private String id;

    @TableField("user_name")
    private String userName;

    @TableField("user_id")
    private String userId;

    @TableField("age")
    private Integer age;

    @TableField("note")
    private String note;

    @TableField("createTime")
    private Date createTime;

    @TableField("createUser")
    private String createUser;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getCreateUser() {
        return createUser;
    }

    public void setCreateUser(String createUser) {
        this.createUser = createUser;
    }

    @Override
    public String toString() {
        return "IdapTestUser{" +
        "id = " + id +
        ", userName = " + userName +
        ", userId = " + userId +
        ", age = " + age +
        ", note = " + note +
        ", createTime = " + createTime +
        ", createUser = " + createUser +
        "}";
    }
}
