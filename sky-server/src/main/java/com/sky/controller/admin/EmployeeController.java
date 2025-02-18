package com.sky.controller.admin;

import cn.dev33.satoken.stp.StpUtil;
import com.sky.constant.MessageConstant;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.entity.Employee;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.EmployeeService;
import com.sky.vo.EmployeeLoginVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * 员工管理
 */
@RestController
@RequestMapping("/admin/employee")
@Slf4j
public class EmployeeController {

    @Resource
    private EmployeeService employeeService;

    /**
     * 登录
     *
     * @param employeeLoginDTO
     * @return
     */
    @PostMapping("/login")
    public Mono<Result<EmployeeLoginVO>> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
        log.info("员工登录：{}", employeeLoginDTO);

        return employeeService.login(employeeLoginDTO)
                        .flatMap(employee -> {
                            StpUtil.login(employee.getId(), "PC");
                            EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
                                    .id(employee.getId())
                                    .userName(employee.getUsername())
                                    .name(employee.getName())
                                    .token(StpUtil.getTokenValue())
                                    .build();
                            return Mono.just(Result.success(employeeLoginVO));
                        });
    }

    /**
     * 退出
     * @return
     */
    @GetMapping("/logout")
    public Mono<Result<String>> logout() {
        log.info("用户发起注销请求");
        return Mono.fromRunnable(() -> {
                    StpUtil.logout();
                    log.info("用户注销成功");
                })
                .thenReturn(Result.success(MessageConstant.USER_LOGOUT));
    }

    /**
     * 新增员工
     * @param employeeDTO
     * @return
     */
    @PostMapping
    public Mono<Result<String>> addEmployee(@RequestBody EmployeeDTO employeeDTO) {
        return employeeService.addEmployee(employeeDTO);
    }

    @GetMapping("/page")
    public Mono<Result<PageResult>> page(@RequestParam(value = "page", defaultValue = "1") int page,
                                   @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
                                   @RequestParam(value = "name", required = false) String name) {
        return employeeService.page(page, pageSize, name);
    }

    /**
     * 更新用户状态
     * @param available
     * @param id
     * @return
     */
    @GetMapping("/status/{available}")
    public Mono<Result<String>> editStatus(@PathVariable("available") Integer available, @RequestParam("id") Long id) {
        return employeeService.editStatus(available, id);
    }

    /**
     * 根据员工id查找用户
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Mono<Result<Employee>> getEmployeeById(@PathVariable("id") Long id) {
        return employeeService.getEmployeeById(id);
    }

    /**
     * 编辑员工信息
     * @param employeeDTO
     * @return
     */
    @PutMapping
    public Mono<Result<String>> editEmployee(@RequestBody EmployeeDTO employeeDTO) {
        return employeeService.editEmployee(employeeDTO);
    }

}
