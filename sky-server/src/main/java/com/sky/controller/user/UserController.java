package com.sky.controller.user;

import com.sky.dto.UserLoginDTO;
import com.sky.result.Result;
import com.sky.service.UserService;
import com.sky.vo.UserLoginVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import reactor.core.publisher.Mono;

/**
 * @author: cyy
 * @create: 2025-02-18 13:55
 **/
@RestController
@RequestMapping("/user/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 微信登陆
     * @param userLoginDTO
     * @return
     */
    @PostMapping("/login")
    public Mono<Result<UserLoginVO>> login(@RequestBody UserLoginDTO userLoginDTO) {
        return userService.login(userLoginDTO);
    }
}
