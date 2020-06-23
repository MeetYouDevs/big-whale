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
@Table(name = "monitor")
public class Monitor {

    @Id
    @GenericGenerator(name = "idGenerator", strategy = "uuid")
    @GeneratedValue(generator = "idGenerator")
    private String id;
    private Integer type;
    private Integer status;
    private String cron;
    private String uid;
    private String scriptId;
    /**
     * spark 挤压批次
     * flink 背压监控的任务阻塞次数
     */
    private Integer waitingBatches;
    private Boolean autoRestart;
    /**
     * 是否异常重启
     */
    private Boolean exAutoRestart;
    private Boolean sendMail;
    /**
     * 多条数据用,分割
     */
    private String dingdingHooks;
    private Date createTime;
    private Date updateTime;
    private Date executeTime;

}
