package com.meiyouframework.bigwhale.dto;

import java.io.Serializable;

/**
 * @author Suxy
 * @date 2019/10/28
 * @description file description
 */
public abstract class AbstractPageDto implements Serializable {

    public int pageNo;
    public int pageSize;
    public int limit;

    public abstract String validate();

    public int getPageNo() {
        return this.pageNo;
    }

    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
    }

    public int getPageSize() {
        return this.pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getLimit() {
        return this.limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

}
