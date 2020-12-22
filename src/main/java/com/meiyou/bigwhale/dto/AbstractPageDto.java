package com.meiyou.bigwhale.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * @author Suxy
 * @date 2019/10/28
 * @description file description
 */
public abstract class AbstractPageDto implements Serializable {

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public int pageNo;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public int pageSize;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public int limit;

    public abstract String validate();

}
