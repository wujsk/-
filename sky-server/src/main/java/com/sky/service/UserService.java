package com.sky.service;

import com.sky.dto.UserLoginDTO;
import com.sky.result.Result;
import com.sky.vo.UserLoginVO;
import reactor.core.publisher.Mono;

public interface UserService {

    Mono<Result<UserLoginVO>> login(UserLoginDTO userLoginDTO);

}
