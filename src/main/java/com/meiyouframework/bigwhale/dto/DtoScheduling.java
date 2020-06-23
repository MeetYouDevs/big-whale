package com.meiyouframework.bigwhale.dto;

import com.meiyouframework.bigwhale.common.Constant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoScheduling extends AbstractPageDto {

    private String id;
    /**
     * 周期
     */
    private Integer cycle;
    private Integer intervals;
    private Integer minute;
    private Integer hour;
    private List<String> week;
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
    private List<String> subScriptIds;
    private String uid;
    private Boolean repeatSubmit;
    private Boolean sendMail;
    private List<String> dingdingHooks;

    @Override
    public String validate() {
        if (StringUtils.isBlank(scriptId)) {
            return "脚本不能为空";
        }
        if (repeatSubmit == null) {
            return "可重复提交必须选择";
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
        List<String> subScriptIds = this.getSubScriptIds();
        if (!CollectionUtils.isEmpty(subScriptIds)) {
            Set<String> subScriptIdsSet = new HashSet<>(subScriptIds);
            if (subScriptIds.size() != subScriptIdsSet.size()) {
                return "子脚本依赖配置有误";
            }
        }
        return null;
    }

}
