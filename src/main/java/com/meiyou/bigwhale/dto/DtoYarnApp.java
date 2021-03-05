package com.meiyou.bigwhale.dto;

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

    private Integer id;
    private Integer clusterId;
    private Integer userId;
    private String appId;
    private String user;
    private String name;
    private String queue;
    private String state;
    private String finalStatus;
    private String trackingUrl;
    private String applicationType;
    private Date startedTime;
    private Date refreshTime;

    @Override
    public String validate() {
        return null;
    }
}
