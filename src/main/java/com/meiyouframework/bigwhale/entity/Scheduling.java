package com.meiyouframework.bigwhale.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "scheduling")
public class Scheduling {

    @Id
    @GenericGenerator(name = "idGenerator", strategy = "uuid")
    @GeneratedValue(generator = "idGenerator")
    private String id;
    /**
     * 周期
     */
    private Integer cycle;
    private Integer intervals;
    private Integer minute;
    private Integer hour;
    /**
     * 多条数据用,分割
     */
    private String week;
    /**
     * cron表达式
     */
    private String cron;
    private Date createTime;
    private Date updateTime;
    private Date startTime;
    private Date endTime;
    private Date lastExecuteTime;
    private Integer status;
    private String scriptId;
    /**
     * 多条数据用,分割
     */
    private String subScriptIds;
    private String uid;
    /**
     * 可重复提交
     */
    private Boolean repeatSubmit;
    private Boolean sendMail;
    /**
     * 多条数据用,分割
     */
    private String dingdingHooks;
}
