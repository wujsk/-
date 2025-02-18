package com.sky.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.entity.Employee;
import com.sky.exception.*;
import com.sky.repository.EmployeeRepository;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.EmployeeService;
import com.sky.utils.SqlUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class EmployeeServiceImpl implements EmployeeService {

    @Resource
    private EmployeeRepository employeeRepository;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     *
     * @return
     */
    @Override
    public Mono<Employee> login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        // 1、根据用户名查询数据库中的数据
        return employeeRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND)))
                .flatMap(employee -> {
                    // 账号存在
                    if (employee.getStatus().equals(StatusConstant.DISABLE)) {
                        // 账号被锁定
                        return Mono.error(new AccountLockedException(MessageConstant.ACCOUNT_LOCKED));
                    }
                    if (!BCrypt.checkpw(password, employee.getPassword())) {
                        // 密码错误
                        return Mono.error(new PasswordErrorException(MessageConstant.PASSWORD_ERROR));
                    }
                    return Mono.just(employee);
                });
    }

    @Override
    public Mono<Result<String>> addEmployee(EmployeeDTO employeeDTO) {
        return Mono.justOrEmpty(employeeDTO)
                .switchIfEmpty(Mono.error(new NullPointerException("新增员工失败")))
                .map(EmployeeDTO::getUsername)
                .switchIfEmpty(Mono.error(new RuntimeException("请输入账号")))
                .map(s -> {
                    Employee employee = BeanUtil.copyProperties(employeeDTO, Employee.class);
                    employee.setPassword(BCrypt.hashpw(PasswordConstant.DEFAULT_PASSWORD));
                    employee.setStatus(StatusConstant.ENABLE);
                    employee.setCreateTime(LocalDateTime.now());
                    employee.setUpdateTime(LocalDateTime.now());
                    employee.setCreateUser(StpUtil.getLoginIdAsLong());
                    employee.setUpdateUser(StpUtil.getLoginIdAsLong());
                    return employee;
                })
                .flatMap(employee -> employeeRepository.save(employee))
                .flatMap(employee -> Mono.just(Result.success("新增成功")))
                .onErrorResume(e -> {
                    log.info("出现异常: {}", e.getMessage());
                    return Mono.just(Result.error("新增失败"));
                });
    }

    @Override
    public Mono<Result<PageResult>> page(int page, int pageSize, String name) {
        if (page < 1 || pageSize < 1) {
            return Mono.error(new BaseException("页码和页大小必须大于0"));
        }
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        name = StringUtils.isBlank(name) ? "%" : SqlUtil.getLikeSql(name);
        return employeeRepository.findAllByNameIsLike(name, pageable)
                .collectList()
                .zipWith(employeeRepository.countByNameIsLike(name))
                .map(tuple -> {
                    List<Employee> employees = tuple.getT1();
                    long count = tuple.getT2();
                    PageResult pageResult = new PageResult();
                    pageResult.setTotal(count);
                    pageResult.setRecords(employees);
                    return Result.success(pageResult);
                })
                .onErrorResume(e -> {
                    log.info("出现异常: {}", e.getMessage());
                    return Mono.error(new SystemException("系统错误，请稍后重试"));
                });
    }

    @Override
    public Mono<Result<String>> editStatus(Integer available, Long id) {
        return Mono.justOrEmpty(id)
                .switchIfEmpty(Mono.error(new BaseException("请选择你要更新的用户")))
                .map(idx -> {
                    Employee employee = new Employee();
                    employee.setId(idx);
                    employee.setStatus(available);
                    employee.setUpdateTime(LocalDateTime.now());
                    employee.setUpdateUser(StpUtil.getLoginIdAsLong());
                    return employee;
                })
                .flatMap(employee -> employeeRepository.updateEmployee(employee))
                .flatMap(count -> {
                    if (count > 0) {
                        return Mono.just(Result.success("更新成功"));
                    }
                    return Mono.error(new BaseException("未找到要更新的用户"));
                })
                .onErrorResume(e -> {
                    log.info("出现异常: {}", e.getMessage());
                    if (e instanceof DuplicateKeyException) {
                        return Mono.just(Result.error("账号已存在"));
                    }
                    return Mono.just(Result.error(("系统错误，请稍后重试")));
                });
    }

    @Override
    public Mono<Result<Employee>> getEmployeeById(Long id) {
        return Mono.justOrEmpty(id)
                .switchIfEmpty(Mono.error(new BaseException("请选择你要查询的员工")))
                .flatMap(employeeRepository::findById)
                .switchIfEmpty(Mono.error(new BaseException("未找到该员工")))
                .flatMap(employee -> Mono.just(Result.success(employee)))
                .onErrorResume(e -> {
                    log.info("出现异常: {}", e.getMessage());
                    return Mono.error(new SystemException("系统错误，请稍后重试"));
                });
    }

    @Override
    public Mono<Result<String>> editEmployee(EmployeeDTO employeeDTO) {
        return employeeRepository.findById(employeeDTO.getId())
                .switchIfEmpty(Mono.error(new BaseException("员工不存在")))
                .map(employee -> {
                    Employee emp = BeanUtil.copyProperties(employeeDTO, Employee.class);
                    emp.setUpdateTime(LocalDateTime.now());
                    emp.setUpdateUser(StpUtil.getLoginIdAsLong());
                    return emp;
                })
                .flatMap(employeeRepository::updateEmployee)
                .flatMap(count -> {
                    if (count > 0) {
                        return Mono.just(Result.success("更新成功"));
                    }
                    return Mono.just(Result.<String>error("更新失败"));
                })
                .onErrorResume(e -> {
                    log.info("出现异常: {}", e.getMessage());
                    if (e instanceof DuplicateKeyException) {
                        return Mono.just(Result.error("账号已存在"));
                    }
                    return Mono.error(new SystemException("系统错误，请稍后重试"));
                });
    }

}
