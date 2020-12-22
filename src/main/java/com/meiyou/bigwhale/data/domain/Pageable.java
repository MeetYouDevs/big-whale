package com.meiyou.bigwhale.data.domain;

/**
 * @author Suxy
 * @date 2020/3/17
 * @description file description
 */
public interface Pageable extends org.springframework.data.domain.Pageable {

    String getFilters();

}
