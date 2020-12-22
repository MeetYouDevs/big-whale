package com.meiyou.bigwhale.data.domain;

import org.springframework.data.domain.Sort;

/**
 * @author Suxy
 * @date 2020/3/17
 * @description file description
 */
public class PageRequest extends org.springframework.data.domain.PageRequest implements Pageable {

    private final String filters;

    public PageRequest(int page, int size, String filters) {
        this(page, size, filters, null);
    }

    public PageRequest(int page, int size, String filters, Sort.Direction direction, String... properties) {
        this(page, size, filters, new Sort(direction, properties));
    }

    public PageRequest(int page, int size, String filters, Sort sort) {
        super(page, size, sort);
        this.filters = filters;
    }

    @Override
    public String getFilters() {
        return filters;
    }

}
