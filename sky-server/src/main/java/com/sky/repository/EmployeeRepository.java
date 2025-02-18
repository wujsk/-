package com.sky.repository;

import com.sky.entity.Employee;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author: cyy
 * @create: 2025-02-17 21:38
 **/
@Repository
public interface EmployeeRepository extends R2dbcRepository <Employee, Long>{

    /**
     * 根据用户名查询员工
     * @param username
     * @return
     */
    @Query("select * from employee where username = :username")
    Mono<Employee> findByUsername(String username);

    /**
     * 根据名称查询员工列表
     * @param name
     * @return
     */
    Flux<Employee> findAllByNameIsLike(String name, Pageable pageable);

    /**
     * 根据名称查询员工数量
     * @param name
     * @return
     */
    Mono<Long> countByNameIsLike(String name);

    @Modifying
    @Query("UPDATE employee SET " +
            "username = CASE WHEN :#{#employee.username} IS NULL THEN username ELSE :#{#employee.username} END, " +
            "name = CASE WHEN :#{#employee.name} IS NULL THEN name ELSE :#{#employee.name} END, " +
            "password = CASE WHEN :#{#employee.password} IS NULL THEN password ELSE :#{#employee.password} END, " +
            "phone = CASE WHEN :#{#employee.phone} IS NULL THEN phone ELSE :#{#employee.phone} END, " +
            "sex = CASE WHEN :#{#employee.sex} IS NULL THEN sex ELSE :#{#employee.sex} END, " +
            "id_number = CASE WHEN :#{#employee.idNumber} IS NULL THEN id_number ELSE :#{#employee.idNumber} END, " +
            "status = CASE WHEN :#{#employee.status} IS NULL THEN status ELSE :#{#employee.status} END, " +
            "update_time = CASE WHEN :#{#employee.updateTime} IS NULL THEN update_time ELSE :#{#employee.updateTime} END, " +
            "update_user = CASE WHEN :#{#employee.updateUser} IS NULL THEN update_user ELSE :#{#employee.updateUser} END " +
            "WHERE id = :#{#employee.id}")
    Mono<Integer> updateEmployee(Employee employee);
}
