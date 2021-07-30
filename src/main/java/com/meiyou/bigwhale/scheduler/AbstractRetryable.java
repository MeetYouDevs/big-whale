package com.meiyou.bigwhale.scheduler;

import com.meiyou.bigwhale.common.Constant;
import com.meiyou.bigwhale.entity.Schedule;
import com.meiyou.bigwhale.entity.ScriptHistory;
import com.meiyou.bigwhale.service.ScheduleService;
import com.meiyou.bigwhale.service.ScriptHistoryService;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

/**
 * @author Suxy
 * @date 2019/9/11
 * @description file description
 */
public abstract class AbstractRetryable extends AbstractNoticeable {

    @Autowired
    private ScriptHistoryService scriptHistoryService;
    @Autowired
    private ScheduleService scheduleService;

    /**
     * 重试当前节点
     * @param scriptHistory
     * @param errorType
     */
    protected void retryCurrentNode(ScriptHistory scriptHistory, String errorType) {
        notice(scriptHistory, errorType);
        boolean retryable = scriptHistory.getScheduleId() != null &&
                scriptHistory.getScheduleFailureHandle() != null &&
                !"UNKNOWN".equals(scriptHistory.getJobFinalStatus());
        if (retryable) {
            Schedule schedule = scheduleService.findById(scriptHistory.getScheduleId());
            if (schedule == null || schedule.getUpdateTime().after(scriptHistory.getBusinessTime())) {
                // 过期任务不重试
                return;
            }
            String [] scheduleFailureHandleArr = scriptHistory.getScheduleFailureHandle().split(";");
            int retries = Integer.parseInt(scheduleFailureHandleArr[0]);
            int intervals = Integer.parseInt(scheduleFailureHandleArr[1]);
            int currRetries = Integer.parseInt(scheduleFailureHandleArr[2]);
            if (currRetries >= retries) {
                return;
            }
            Date startAt = DateUtils.addMinutes(new Date(), intervals);
            ScriptHistory retryScriptHistory = new ScriptHistory();
            BeanUtils.copyProperties(scriptHistory, retryScriptHistory);
            retryScriptHistory.reset();
            retryScriptHistory.setScheduleFailureHandle(retries + ";" + intervals + (currRetries + 1));
            retryScriptHistory.setScheduleRetry(true);
            retryScriptHistory.setCreateTime(new Date());
            retryScriptHistory.updateState(Constant.JobState.UN_CONFIRMED_);
            retryScriptHistory.updateState(Constant.JobState.TIME_WAIT_);
            retryScriptHistory.setDelayTime(startAt);
            scriptHistoryService.save(retryScriptHistory);
        }
    }

}
