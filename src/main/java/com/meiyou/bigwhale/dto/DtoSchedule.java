package com.meiyou.bigwhale.dto;

import com.meiyou.bigwhale.common.Constant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.StringUtils;

import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoSchedule extends AbstractPageDto {

    private Integer id;
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
    private Date realFireTime;
    private Date needFireTime;
    private Date nextFireTime;
    private Date createTime;
    private Integer createBy;
    private Date updateTime;
    private Integer updateBy;
    private String keyword;


    private List<DtoScript> scripts;

    /**
     * 模糊搜字段
     */
    private String instance;

    @Deprecated
    private List<String> subScriptIds;

    @Override
    public String validate() {
        if (StringUtils.isBlank(topology)) {
            return "拓扑不能为空";
        }
        if (startTime == null || endTime == null) {
            return "请选择时间范围";
        }
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
        for (DtoScript script : scripts) {
            String msg = script.validate();
            if (msg != null) {
                return msg;
            }
        }
        return null;
    }

}
