package com.meiyou.bigwhale.entity;

import com.meiyou.bigwhale.common.Constant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

/**
 * @author Suxy
 * @date 2021/2/5
 * @description file description
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "monitor")
public class Monitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer cycle;
    private Integer intervals;
    private Integer minute;
    private Integer hour;
    /**
     * 多条数据用,分割
     */
    private String week;
    private String cron;
    private Boolean exRestart;
    private Integer waitingBatches;
    private Boolean blockingRestart;
    private Boolean sendEmail;
    /**
     * 多条数据用,分割
     */
    private String dingdingHooks;
    private Boolean enabled;
    private Date realFireTime;

    public String generateCron() {
        if (cron != null) {
            return cron;
        } else {
            String cron = null;
            if (cycle == Constant.TIMER_CYCLE_MINUTE) {
                cron = "0 */" + intervals + " * * * ? *";
            } else if (cycle == Constant.TIMER_CYCLE_HOUR) {
                cron = "0 " + minute + " * * * ? *";
            } else if (cycle == Constant.TIMER_CYCLE_DAY) {
                cron = "0 " + minute + " " + hour + " * * ? *";
            } else if (cycle == Constant.TIMER_CYCLE_WEEK) {
                cron = "0 " + minute + " " + hour + " ? * " + week + " *";
            }
            if (cron == null) {
                throw new IllegalArgumentException("cron expression is incorrect");
            }
            return cron;
        }
    }

}
