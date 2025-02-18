package com.sky.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson2.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.properties.WeChatProperties;
import com.sky.repository.UserRepository;
import com.sky.result.Result;
import com.sky.service.UserService;
import com.sky.vo.UserLoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @program: sky-take-out
 * @description:
 * @author: 酷炫焦少
 * @create: 2024-11-17 21:47
 **/
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    public final static String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

    @Resource
    private WeChatProperties weChatProperties;

    @Resource
    private WebClient webClient;

    @Resource
    private UserRepository userRepository;

    public Mono<User> wxLogin(UserLoginDTO userLoginDTO) {
        //调用微信登录接口，获得当前用户的openid
        Map<String, String> param = new HashMap<>();
        param.put("appid", weChatProperties.getAppid());
        param.put("secret", weChatProperties.getSecret());
        param.put("js_code", userLoginDTO.getCode());
        param.put("grant_type", "authorization_code");
        return webClient.get()
                .uri(WX_LOGIN, param)
                .retrieve()
                .bodyToMono(String.class)
                .map(json -> {
                    JSONObject jsonObject = JSONObject.parseObject(json);
                    String openid = jsonObject.getString("openid");
                    if (Objects.isNull(openid)) {
                        log.error("微信登录失败，返回结果：{}", json);
                        throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
                    }
                    return openid;
                })
                .flatMap(openid -> {
                    return userRepository.findUserByOpenid(openid)
                            .switchIfEmpty(Mono.defer(() -> {
                                User user = User.builder()
                                        .openid(openid)
                                        .createTime(LocalDateTime.now())
                                        .build();
                                return userRepository.save(user)
                                        .then(Mono.just(user))
                                        .onErrorResume(e -> {
                                            log.error("用户注册失败，openid: {}", openid, e);
                                            return Mono.error(new RuntimeException("用户注册失败"));
                                        });
                            }));
                });
    }

    @Override
    public Mono<Result<UserLoginVO>> login(UserLoginDTO userLoginDTO) {
        return wxLogin(userLoginDTO)
                .flatMap(user -> {
                    StpUtil.login(user.getId());
                    String token = StpUtil.getTokenValue();
                    UserLoginVO userLoginVO = UserLoginVO.builder()
                            .id(user.getId())
                            .openid(user.getOpenid())
                            .token(token)
                            .build();
                    return Mono.just(Result.success(userLoginVO));
                })
                .switchIfEmpty(Mono.just(Result.error("用户不存在")));
    }

}
