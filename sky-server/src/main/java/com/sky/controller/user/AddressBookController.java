package com.sky.controller.user;

import jakarta.annotation.Resource;
import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.result.Result;
import com.sky.service.AddressBookService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author: cyy
 * @create: 2025-02-18 13:47
 **/
@RestController
@RequestMapping("/user/addressBook")
public class AddressBookController {

    @Resource
    private AddressBookService addressBookService;

    /**
     * 查询当前登录用户的所有地址信息
     *
     * @return
     */
    @GetMapping("/list")
    public Mono<Result<List<AddressBook>>> list() {
        AddressBook addressBook = new AddressBook();
        addressBook.setUserId(BaseContext.getCurrentId());
        return addressBookService.list(addressBook);
    }

    /**
     * 新增地址
     *
     * @param addressBook
     * @return
     */
    @PostMapping
    public Mono<Result<String>> save(@RequestBody AddressBook addressBook) {
        return addressBookService.save(addressBook);
    }

    @GetMapping("/{id}")
    public Mono<Result<AddressBook>> getById(@PathVariable Long id) {
        return addressBookService.getById(id);
    }

    /**
     * 根据id修改地址
     *
     * @param addressBook
     * @return
     */
    @PutMapping
    public Mono<Result<String>> update(@RequestBody AddressBook addressBook) {
        return addressBookService.update(addressBook);
    }

    /**
     * 设置默认地址
     *
     * @param addressBook
     * @return
     */
    @PutMapping("/default")
    public Mono<Result<String>> setDefault(@RequestBody AddressBook addressBook) {
        return addressBookService.setDefault(addressBook);
    }

    /**
     * 根据id删除地址
     *
     * @param id
     * @return
     */
    @DeleteMapping
    public Mono<Result<String>> deleteById(Long id) {
        return addressBookService.deleteById(id);
    }

    /**
     * 查询默认地址
     */
    @GetMapping("default")
    public Mono<Result<AddressBook>> getDefault() {
        //SQL:select * from address_book where user_id = ? and is_default = 1
        AddressBook addressBook = new AddressBook();
        addressBook.setIsDefault(1);
        addressBook.setUserId(BaseContext.getCurrentId());
        return addressBookService.list(addressBook)
                .flatMap(result -> {
                    List<AddressBook> data = result.getData();
                    if (data != null && !data.isEmpty()) {
                        return Mono.just(Result.success(data.get(0)));
                    }
                    return Mono.just(Result.error("没有查询到默认地址"));
                });
    }
}
