package com.sky.service;

import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.entity.Employee;
import com.sky.result.PageResult;
import com.sky.result.Result;
import reactor.core.publisher.Mono;

public interface EmployeeService {

    /**
     * 员工登录
     * @param employeeLoginDTO
     * @return
     */
    Mono<Employee> login(EmployeeLoginDTO employeeLoginDTO);

    /**
     * 新增员工
     * @param employeeDTO
     * @return
     */
    Mono<Result<String>> addEmployee(EmployeeDTO employeeDTO);

    /**
     * 分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    Mono<Result<PageResult>> page(int page, int pageSize, String name);

    /**
     * 更新用户状态
     * @param available
     * @param id
     * @return
     */
    Mono<Result<String>> editStatus(Integer available, Long id);

    /**
     * 根据员工id查找用户
     * @param id
     * @return
     */
    Mono<Result<Employee>> getEmployeeById(Long id);

    /**
     * 编辑员工信息
     * @param employeeDTO
     * @return
     */
    Mono<Result<String>> editEmployee(EmployeeDTO employeeDTO);
}
