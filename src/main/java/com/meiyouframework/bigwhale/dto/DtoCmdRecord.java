package com.meiyouframework.bigwhale.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoCmdRecord extends AbstractPageDto {

    private String id;
    private String schedulingId;
    private String monitorId;
    private String scriptId;
    private Integer status;
    private String clusterId;
    private String agentId;
    private String uid;
    private String content;

    /**
     * 查询属性
     */
    private Date start;
    private Date end;

    @Override
    public String validate() {
        return null;
    }
}
