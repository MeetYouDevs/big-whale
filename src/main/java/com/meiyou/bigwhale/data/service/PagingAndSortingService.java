package com.meiyou.bigwhale.data.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.Serializable;

/**
 * @author Suxy
 * @date 2019/9/11
 * @description file description
 */
public interface PagingAndSortingService<T, ID extends Serializable> {

    <S extends T> S save(S entity);

    <S extends T> Iterable<S> saveAll(Iterable<S> entities);

    T findById(ID id);

    boolean existsById(ID id);

    Iterable<T> findAll();

    Iterable<T> findAllById(Iterable<ID> ids);

    long count();

    void deleteById(ID id);

    void delete(T entity);

    void deleteAll(Iterable<? extends T> entities);

    void deleteAll();

    Iterable<T> findAll(Sort sort);

    Page<T> findAll(Pageable pageable);

}
