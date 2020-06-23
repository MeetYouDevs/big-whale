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
public class DtoYarnApp extends AbstractPageDto {

    private String id;
    private String uid;
    private String scriptId;
    private String clusterId;
    private Date updateTime;
    private String queue;
    private String name;
    private String appId;
    private String user;
    private String state;
    private String finalStatus;
    private String trackingUrl;
    private String applicationType;
    private Date startedTime;

    @Override
    public String validate() {
        return null;
    }
}
