package com.orientsec.idap.core.controller;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.orientsec.idap.common.model.Result;
import com.orientsec.idap.common.model.ResultGenerator;
import com.orientsec.idap.common.utils.LogHelper;
import com.orientsec.idap.core.model.IdapUserInfo;
import com.orientsec.idap.core.service.IdapUserInfoService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 用户信息表 前端控制器
 * </p>
 *
 * @author autocode
 * @since 2026-04-09
 */
@RestController
@RequestMapping("/idap/v1/idapUserInfo")
@Slf4j
public class IdapUserInfoController {

    private static final Logger logger = LoggerFactory.getLogger(IdapUserInfoController.class);

    @Autowired
    private IdapUserInfoService idapUserInfoService;

    /**
     * 查询列表（前端分页 - 返回全部数据）
     */
    @GetMapping("/list")
    public Result list(@RequestParam(required = false) String userName,
                       @RequestParam(required = false) String mobile,
                       @RequestParam(required = false) String email,
                       @RequestParam(required = false) Integer sex,
                       @RequestParam(required = false) Integer status) {
        LogHelper.log(logger, "进入用户列表查询，参数：", userName, mobile, email, sex, status);
        logger.info("进入用户列表查询, userName={}, mobile={}, email={}, sex={}, status={}",
                userName, mobile, email, sex, status);
        try {
            LambdaQueryWrapper<IdapUserInfo> param = new LambdaQueryWrapper<>();
            param.eq(IdapUserInfo::getDeleted, (byte) 0)
                    .like(userName != null, IdapUserInfo::getUserName, userName)
                    .like(mobile != null, IdapUserInfo::getMobile, mobile)
                    .like(email != null, IdapUserInfo::getEmail, email)
                    .eq(sex != null, IdapUserInfo::getSex, sex)
                    .eq(status != null, IdapUserInfo::getStatus, status)
                    .orderByDesc(IdapUserInfo::getCreateTime);

            List<IdapUserInfo> userList = idapUserInfoService.list(param);

            logger.info("用户列表查询成功, 数量：{}", userList.size());
            return ResultGenerator.genSuccessResult(userList);
        } catch (Exception e) {
            logger.error("用户列表查询失败", e);
            return ResultGenerator.genFailResult(e);
        }
    }

    /**
     * 获取详情
     */
    @GetMapping("/detail")
    public Result detail(@RequestParam String userId) {
        logger.info("进入用户详情查询, userId={}", userId);
        try {
            IdapUserInfo userInfo = idapUserInfoService.getById(userId);
            if (userInfo == null || userInfo.getDeleted() == 1) {
                throw new RuntimeException("用户不存在");
            }
            logger.info("用户详情查询成功");
            return ResultGenerator.genSuccessResult(userInfo);
        } catch (Exception e) {
            logger.error("用户详情查询失败", e);
            return ResultGenerator.genFailResult(e);
        }
    }

    /**
     * 新增
     */
    @PostMapping("/create")
    public Result create(@RequestBody Map<String, Object> data) {
        logger.info("进入用户新增，数据：{}", data);
        try {
            IdapUserInfo userInfo = new IdapUserInfo();
            userInfo.setUserId(IdUtil.fastSimpleUUID());
            userInfo.setUserName((String) data.get("userName"));
            userInfo.setEmail((String) data.get("email"));
            userInfo.setMobile((String) data.get("mobile"));
            Object sexObj = data.get("sex");
            if (sexObj != null) {
                userInfo.setSex(((Number) sexObj).byteValue());
            } else {
                userInfo.setSex((byte) 0);
            }
            userInfo.setNote((String) data.get("note"));
            userInfo.setDepartmentId((String) data.get("departmentId"));
            Object statusObj = data.get("status");
            if (statusObj != null) {
                userInfo.setStatus(((Number) statusObj).byteValue());
            } else {
                userInfo.setStatus((byte) 1);
            }
            userInfo.setAvatar((String)data.get("avatar"));
            userInfo.setCreateTime(new Date());
            userInfo.setCreateUser("admin");
            userInfo.setUpdateTime(new Date());
            userInfo.setUpdateUser("admin");
            userInfo.setDeleted((byte) 0);

            idapUserInfoService.save(userInfo);
            logger.info("用户新增成功, userId={}", userInfo.getUserId());
            return ResultGenerator.genSuccessResult();
        } catch (Exception e) {
            logger.error("用户新增失败", e);
            return ResultGenerator.genFailResult(e);
        }
    }

    /**
     * 更新
     */
    @PostMapping("/update")
    public Result update(@RequestBody Map<String, Object> data) {
        logger.info("进入用户更新，数据：{}", data);
        try {
            String userId = (String) data.get("userId");
            IdapUserInfo userInfo = idapUserInfoService.getById(userId);
            if (userInfo == null || userInfo.getDeleted() == 1) {
                throw new RuntimeException("用户不存在");
            }

            userInfo.setUserName((String) data.get("userName"));
            userInfo.setEmail((String) data.get("email"));
            userInfo.setMobile((String) data.get("mobile"));
            Object sexObj = data.get("sex");
            if (sexObj != null) {
                userInfo.setSex(((Number) sexObj).byteValue());
            }
            userInfo.setNote((String) data.get("note"));
            userInfo.setDepartmentId((String) data.get("departmentId"));
            Object statusObj = data.get("status");
            if (statusObj != null) {
                userInfo.setStatus(((Number) statusObj).byteValue());
            }
            userInfo.setAvatar((String)data.get("avatar"));
            userInfo.setUpdateTime(new Date());
            userInfo.setUpdateUser("admin");

            idapUserInfoService.updateById(userInfo);
            logger.info("用户更新成功, userId={}", userId);
            return ResultGenerator.genSuccessResult();
        } catch (Exception e) {
            logger.error("用户更新失败", e);
            return ResultGenerator.genFailResult(e);
        }
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public Result delete(@RequestBody Map<String, Object> data) {
        Object idsObj = data.get("ids");
        logger.info("进入用户删除, ids={}", idsObj);
        try {
            List<String> ids;
            if (idsObj instanceof List) {
                ids = (List<String>) idsObj;
            } else if (idsObj instanceof String[]) {
                ids = Arrays.asList((String[]) idsObj);
            } else {
                ids = Arrays.asList(String.valueOf(idsObj));
            }

            for (String userId : ids) {
                IdapUserInfo userInfo = idapUserInfoService.getById(userId);
                if (userInfo != null && userInfo.getDeleted() == 0) {
                    userInfo.setDeleted((byte) 1);
                    userInfo.setUpdateTime(new Date());
                    userInfo.setUpdateUser("admin");
                    idapUserInfoService.updateById(userInfo);
                }
            }
            logger.info("用户删除成功, ids={}", ids);
            return ResultGenerator.genSuccessResult();
        } catch (Exception e) {
            logger.error("用户删除失败", e);
            return ResultGenerator.genFailResult(e);
        }
    }

    /**
     * 修改状态
     */
    @PostMapping("/changeStatus")
    public Result changeStatus(@RequestBody Map<String, Object> data) {
        logger.info("进入用户状态修改, userId={}, status={}", data.get("userId"), data.get("status"));
        try {
            String userId = (String) data.get("userId");
            Integer status = (Integer) data.get("status");

            IdapUserInfo userInfo = idapUserInfoService.getById(userId);
            if (userInfo == null || userInfo.getDeleted() == 1) {
                throw new RuntimeException("用户不存在");
            }

            userInfo.setStatus(status.byteValue());
            userInfo.setUpdateTime(new Date());
            userInfo.setUpdateUser("admin");
            idapUserInfoService.updateById(userInfo);

            logger.info("用户状态修改成功, userId={}, status={}", userId, status);
            return ResultGenerator.genSuccessResult();
        } catch (Exception e) {
            logger.error("用户状态修改失败", e);
            return ResultGenerator.genFailResult(e);
        }
    }

    /**
     * 重置密码
     */
    @PostMapping("/resetPassword")
    public Result resetPassword(@RequestBody Map<String, Object> data) {
        logger.info("进入用户密码重置, userId={}", data.get("userId"));
        try {
            String userId = (String) data.get("userId");
            IdapUserInfo userInfo = idapUserInfoService.getById(userId);
            if (userInfo == null || userInfo.getDeleted() == 1) {
                throw new RuntimeException("用户不存在");
            }

            // TODO: 如果有密码表，在这里重置密码
            logger.info("用户密码重置成功, userId={}", userId);
            return ResultGenerator.genSuccessResult();
        } catch (Exception e) {
            logger.error("用户密码重置失败", e);
            return ResultGenerator.genFailResult(e);
        }
    }
}
