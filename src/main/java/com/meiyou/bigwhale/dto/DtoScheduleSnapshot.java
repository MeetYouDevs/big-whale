package com.meiyou.bigwhale.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * @author Suxy
 * @date 2021/2/5
 * @description file description
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoScheduleSnapshot extends AbstractPageDto {

    private Integer id;
    private Integer scheduleId;
    private Date snapshotTime;
    private String name;
    private String description;
    private Integer cycle;
    private Integer intervals;
    private Integer minute;
    private Integer hour;
    /**
     * 多条数据用,分割
     */
    private List<String> week;
    private String cron;
    private Date startTime;
    private Date endTime;
    private String topology;
    private Boolean sendEmail;
    /**
     * 多条数据用,分割
     */
    private List<String> dingdingHooks;
    private Boolean enabled;

    @Override
    public String validate() {
        return null;
    }
}
