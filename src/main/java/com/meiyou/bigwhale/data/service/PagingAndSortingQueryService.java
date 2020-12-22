package com.meiyou.bigwhale.data.service;

import com.meiyou.bigwhale.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.io.Serializable;
import java.util.List;

/**
 * @author Suxy
 * @date 2019/9/12
 * @description file description
 */
public interface PagingAndSortingQueryService<T, ID extends Serializable> extends PagingAndSortingService<T, ID> {

    T findOneByQuery(String filters);

    T findOneByQuery(String filters, Sort sort);

    List<T> findByQuery(String filters);

    List<T> findByQuery(String filters, Sort sort);

    Page<T> pageByQuery(Pageable pageable);

    void deleteByQuery(String filters);

}
