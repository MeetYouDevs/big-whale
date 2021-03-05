package com.meiyou.bigwhale.dto;

import com.meiyou.bigwhale.common.Constant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * @author Suxy
 * @date 2021/2/5
 * @description file description
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoMonitor extends AbstractPageDto {

    private Integer id;
    private Integer cycle;
    private Integer intervals;
    private Integer minute;
    private Integer hour;
    /**
     * 多条数据用,分割
     */
    private List<String> week;
    private String cron;
    private Boolean exRestart;
    private Integer waitingBatches;
    private Boolean blockingRestart;
    private Boolean sendEmail;
    /**
     * 多条数据用,分割
     */
    private List<String> dingdingHooks;
    private Boolean enabled;
    private Date realFireTime;

    @Override
    public String validate() {
        if (cron == null) {
            if (this.cycle == Constant.TIMER_CYCLE_MINUTE && intervals == null) {
                return "时间间隔不能为空";
            }
            if (this.cycle == Constant.TIMER_CYCLE_HOUR && this.minute == null) {
                return "分钟不能为空";
            }
            if (this.cycle == Constant.TIMER_CYCLE_DAY && (this.hour == null || this.minute == null)) {
                return "小时、分钟不能为空";
            }
            if (this.cycle == Constant.TIMER_CYCLE_WEEK && (this.week == null || this.hour == null || this.minute == null)) {
                return "周、小时、分钟不能为空";
            }
        }
        return null;
    }

}
