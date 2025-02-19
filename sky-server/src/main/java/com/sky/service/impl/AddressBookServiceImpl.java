package com.sky.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.sky.entity.AddressBook;
import com.sky.exception.BaseException;
import com.sky.repository.AddressBookRepository;
import com.sky.result.Result;
import com.sky.service.AddressBookService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Slf4j
public class AddressBookServiceImpl implements AddressBookService {

    @Resource
    private AddressBookRepository addressBookRepository;

    /**
     * 条件查询
     *
     * @param addressBook
     * @return
     */
    @Override
    public Mono<Result<List<AddressBook>>> list(AddressBook addressBook) {
        return addressBookRepository.list(addressBook)
                .collectList()
                .flatMap(list -> Mono.just(Result.success(list)))
                .onErrorResume(e -> {
                    log.error("查询地址列表失败", e);
                    return Mono.error(new RuntimeException("查询地址列表失败"));
                });
    }

    /**
     * 新增地址
     *
     * @param addressBook
     */
    @Override
    public Mono<Result<String>> save(AddressBook addressBook) {
        return addressBookRepository.save(addressBook)
                .flatMap(add -> Mono.just(Result.success("新增地址成功")))
                .onErrorResume(e -> {
                    log.error("新增地址失败", e);
                    return Mono.error(new RuntimeException("新增地址失败"));
                });
    }

    /**
     * 根据id查询
     *
     * @param id
     * @return
     */
    @Override
    public Mono<Result<AddressBook>> getById(Long id) {
        return addressBookRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("地址不存在")))
                .flatMap(addressBook -> Mono.just(Result.success(addressBook)))
                .onErrorResume(e -> {
                    log.error("根据id查询地址失败", e);
                    return Mono.error(new RuntimeException("根据id查询地址失败"));
                });
    }

    /**
     * 根据id修改地址
     *
     * @param addressBook
     */
    @Override
    public Mono<Result<String>> update(AddressBook addressBook) {
        return addressBookRepository.update(addressBook)
                .flatMap(count -> {
                    if (count < 1) {
                        return Mono.just(Result.success("修改地址失败"));
                    }
                    return Mono.just(Result.success("修改地址成功"));
                })
                .onErrorResume(e -> {;
                    log.error("根据id修改地址失败", e);
                    return Mono.error(new RuntimeException("根据id修改地址失败"));
                });
    }

    /**
     * 根据id删除地址
     *
     * @param id
     */
    @Override
    public Mono<Result<String>> deleteById(Long id) {
        return addressBookRepository.deleteById(id)
                .thenReturn(Result.success("删除地址成功"))
                .onErrorResume(e -> {
                    log.error("根据id删除地址失败", e);
                    return Mono.error(new RuntimeException("根据id删除地址失败"));
                });
    }

    /**
     * 设置默认地址
     *
     * @param addressBook
     */
    @Transactional
    @Override
    public Mono<Result<String>> setDefault(AddressBook addressBook) {
        //1、将当前用户的所有地址修改为非默认地址 update address_book set is_default = ? where user_id = ?
        addressBook.setIsDefault(0);
        addressBook.setUserId(StpUtil.getLoginIdAsLong());
        return addressBookRepository.updateIsDefaultByUserId(addressBook)
                .flatMap(add -> {
                    if (add < 1) {
                        return Mono.error(new BaseException("设置默认地址失败"));
                    }
                    addressBook.setIsDefault(1);
                    return addressBookRepository.update(addressBook)
                            .flatMap(count -> {
                                if (count < 1) {
                                    return Mono.error(new BaseException("设置默认地址失败"));
                                }
                                return Mono.just(Result.success("设置默认地址成功"));
                            });
                })
                .onErrorResume(e -> {
                    log.error("设置默认地址失败", e);
                    return Mono.error(new BaseException("设置默认地址失败"));
                });
    }

}
