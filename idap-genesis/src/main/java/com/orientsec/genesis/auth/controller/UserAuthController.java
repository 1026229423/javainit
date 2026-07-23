package com.orientsec.genesis.auth.controller;


import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.orientsec.genesis.auth.service.GenesisUserService;
import com.orientsec.idap.common.model.Result;
import com.orientsec.idap.common.model.ResultGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.orientsec.genesis.auth.common.model.Menu;
import com.orientsec.genesis.auth.common.model.User;
import com.orientsec.genesis.auth.filter.bjca.filter.GenesisFilter;
import com.orientsec.genesis.auth.filter.bjca.utils.BJCAUtils;
import com.orientsec.genesis.auth.util.GenesisSessionUtils;

import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/auth")
@Slf4j
public class UserAuthController {

    @Resource
    private GenesisUserService genesisUserService;

    @Autowired
    private BJCAUtils tokenUtils;

    @Value("${ca.domainName}")
    private String domainName;

    @Value(value = "${proj.genesis-key:RUIYAN}")
    private String proj_key;

    @GetMapping("/menu_list")
    public Result menu_list() {
        List<Menu> list = genesisUserService.getUserMenus();
        return ResultGenerator.genSuccessResult(list);
    }
    @GetMapping("/whoami")
    public Result whoami() {
        log.info("genesis-key {}",proj_key);
        User user = this.genesisUserService.getCurrentUser();
        log.info("getCurrentUser {}",user.getName());
        return ResultGenerator.genSuccessResult(user);
    }

    @GetMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            String sessionId = request.getRequestedSessionId();
            Subject subject = GenesisSessionUtils.getSubjectBySessionId(sessionId);
            String tokenId = (String) subject.getSession().getAttribute(GenesisFilter.TOKENID);
            if (StringUtils.isNotBlank(tokenId)) {
                //sso logout
                tokenUtils.loginOut(tokenId);
            }
            // shiro logout
            subject.logout();
            response.addHeader("Set-Cookie", BJCAUtils.TOKEN_COOKIE + "=; Path=/; Max-Age=0");
            response.addHeader("Set-Cookie", "gns_session" + "=; Path=/; Max-Age=0");

            response.sendRedirect(domainName); //跳回主页
        } catch (Exception e) {
            response.setStatus(HttpResponseStatus.BAD_REQUEST.code());
        }
    }
}
