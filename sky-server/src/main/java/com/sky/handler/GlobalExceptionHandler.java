package com.sky.handler;

import cn.dev33.satoken.exception.NotLoginException;
import com.sky.constant.MessageConstant;
import com.sky.exception.*;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result<String> exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    @ExceptionHandler
    public Result<String> NotLoginExceptionHandler(NotLoginException ex) {
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(HttpStatus.UNAUTHORIZED.value(), MessageConstant.USER_NOT_LOGIN);
    }

    @ExceptionHandler
    public Result<String> AccountNotFoundExceptionHandler(AccountNotFoundException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    @ExceptionHandler
    public Result<String> PasswordErrorExceptionHandler(PasswordErrorException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    @ExceptionHandler
    public Result<String> AccountLockedExceptionHandler(AccountLockedException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    @ExceptionHandler
    public Result<String> RuntimeExceptionHandler(RuntimeException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    @ExceptionHandler
    public Result<String> SystemExceptionHandler(SystemException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

}
