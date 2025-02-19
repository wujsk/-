package com.sky.repository;

import com.sky.entity.AddressBook;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public interface AddressBookRepository extends R2dbcRepository<AddressBook, Long> {

    /**
     * 条件查询
     * @param addressBook
     * @return
     */
    @Query("SELECT * FROM address_book WHERE " +
            "(:userId IS NULL OR user_id = :userId) AND " +
            "(:consignee IS NULL OR consignee = :consignee) AND " +
            "(:phone IS NULL OR phone = :phone) AND " +
            "(:sex IS NULL OR sex = :sex) AND " +
            "(:provinceCode IS NULL OR province_code = :provinceCode) AND " +
            "(:provinceName IS NULL OR province_name = :provinceName) AND " +
            "(:cityCode IS NULL OR city_code = :cityCode) AND " +
            "(:cityName IS NULL OR city_name = :cityName) AND " +
            "(:districtCode IS NULL OR district_code = :districtCode) AND " +
            "(:districtName IS NULL OR district_name = :districtName) AND " +
            "(:detail IS NULL OR detail = :detail) AND " +
            "(:label IS NULL OR label = :label) AND " +
            "(:isDefault IS NULL OR is_default = :isDefault)")
    Flux<AddressBook> list(AddressBook addressBook);

    /**
     * 根据id查询
     * @param id
     * @return
     */
    Mono<AddressBook> findById(Long id);


    /**
     * 根据id修改
     * @param addressBook
     */
    @Query("UPDATE address_book SET " +
            "user_id = :userId, " +
            "consignee = :consignee, " +
            "phone = :phone, " +
            "sex = :sex, " +
            "province_code = :provinceCode, " +
            "province_name = :provinceName, " +
            "city_code = :cityCode, " +
            "city_name = :cityName, " +
            "district_code = :districtCode, " +
            "district_name = :districtName, " +
            "detail = :detail, " +
            "label = :label, " +
            "is_default = :isDefault " +
            "WHERE id = :id")
    Mono<Integer> update(AddressBook addressBook);

    /**
     * 根据 用户id修改 是否默认地址
     * @param addressBook
     */
    @Query("UPDATE address_book SET is_default = :isDefault WHERE user_id = :userId")
    Mono<Integer> updateIsDefaultByUserId(AddressBook addressBook);

    @Query("SELECT * FROM address_book WHERE " +
            "(:userId IS NULL OR user_id = :userId) AND " +
            "(:id IS NULL OR consignee = :id) AND ")
    Mono<AddressBook> getOne(AddressBook addressBook);
}
