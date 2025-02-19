package com.sky.service;

import com.sky.entity.AddressBook;
import com.sky.result.Result;
import reactor.core.publisher.Mono;

import java.util.List;

public interface AddressBookService {

    Mono<Result<List<AddressBook>>> list(AddressBook addressBook);

    Mono<Result<String>> save(AddressBook addressBook);

    Mono<Result<AddressBook>> getById(Long id);

    Mono<Result<String>> update(AddressBook addressBook);

    Mono<Result<String>> setDefault(AddressBook addressBook);

    Mono<Result<String>> deleteById(Long id);

}
