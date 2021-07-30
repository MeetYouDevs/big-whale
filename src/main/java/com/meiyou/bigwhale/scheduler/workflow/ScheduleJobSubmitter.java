package com.meiyou.bigwhale.scheduler.workflow;

import com.meiyou.bigwhale.common.Constant;
import com.meiyou.bigwhale.entity.ScriptHistory;
import com.meiyou.bigwhale.scheduler.job.ScriptJob;
import com.meiyou.bigwhale.service.ScriptHistoryService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Suxy
 * @date 2020/4/23
 * @description file description
 */
@DisallowConcurrentExecution
public class ScheduleJobSubmitter implements Job {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ScriptHistoryService scriptHistoryService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        updateUnconfirmed();
        submitTimeWait();
    }

    private void updateUnconfirmed() {
        List<ScriptHistory> scriptHistories = scriptHistoryService.findByQuery("scheduleId+;scheduleRunnable=true;state=" + Constant.JobState.UN_CONFIRMED_,
                new Sort(Sort.Direction.ASC, "id"));
        for (ScriptHistory scriptHistory : scriptHistories) {
            boolean switchTimeWait;
            if (scriptHistory.getPreviousScheduleTopNodeId() == null) {
                switchTimeWait = true;
            } else {
                String previousNodeState = previousNodeState(scriptHistory.getScheduleId(), scriptHistory.getScheduleInstanceId(), scriptHistory.getPreviousScheduleTopNodeId());
                switch (previousNodeState) {
                    case Constant.JobState.SUCCEEDED:
                        switchTimeWait = true;
                        break;
                    case Constant.JobState.FAILED:
                        switchTimeWait = false;
                        break;
                    case Constant.JobState.RUNNING:
                    default:
                        continue;
                }
            }
            if (switchTimeWait) {
                scriptHistory.updateState(Constant.JobState.TIME_WAIT_);
                scriptHistory.setDelayTime(scriptHistory.getBusinessTime());
                jdbcTemplate.update("UPDATE " +
                                "   script_history " +
                                "SET " +
                                "   state = ?, " +
                                "   steps = ?, " +
                                "   delay_time = ? " +
                                "WHERE " +
                                "   id = ?;",
                        scriptHistory.getState(), scriptHistory.getSteps(), scriptHistory.getDelayTime(),
                        scriptHistory.getId()
                );
            } else {
                scriptHistory.setScheduleRunnable(false);
                scriptHistoryService.switchScheduleRunnable(scriptHistory.getId(), scriptHistory.getScheduleRunnable());
            }
        }
    }

    private void submitTimeWait() {
        List<ScriptHistory> scriptHistories = scriptHistoryService.findByQuery("scheduleId+;" +
                        "delayTime<=" + DATE_FORMAT.format(new Date()) + ";" +
                        "state=" + Constant.JobState.TIME_WAIT_,
                new Sort(Sort.Direction.ASC, "id"));
        for (ScriptHistory scriptHistory : scriptHistories) {
            scriptHistory.updateState(Constant.JobState.SUBMIT_WAIT);
            scriptHistory.updateState(Constant.JobState.SUBMITTING);
            scriptHistory = scriptHistoryService.save(scriptHistory);
            ScriptJob.build(scriptHistory);
        }
    }

    /**
     * @param scheduleId
     * @param scheduleInstanceId
     * @param previousScheduleTopNodeId
     * @return
     */
    private String previousNodeState(Integer scheduleId, String scheduleInstanceId, String previousScheduleTopNodeId) {
        List<ScriptHistory> scriptHistories = scriptHistoryService.findByQuery("scheduleId=" + scheduleId +
                ";scheduleTopNodeId=" + previousScheduleTopNodeId +
                ";scheduleInstanceId=" + scheduleInstanceId, new Sort(Sort.Direction.DESC, "id"));
        // 判断是否重试完毕
        for (ScriptHistory scriptHistory : scriptHistories) {
            if (scriptHistory.getScheduleFailureHandle() != null) {
                String [] scheduleFailureHandleArr = scriptHistory.getScheduleFailureHandle().split(";");
                int failureRetries = Integer.parseInt(scheduleFailureHandleArr[0]);
                int currFailureRetries = Integer.parseInt(scheduleFailureHandleArr[2]);
                if (currFailureRetries < failureRetries) {
                    // 一般失败后便会进入重试流程，如果最后一个任务结束30S之后还没有进入重试流程，则直接走状态判断流程
                    if ((System.currentTimeMillis() - scriptHistory.getFinishTime().getTime()) < 30000) {
                        return Constant.JobState.RUNNING;
                    }
                }
                break;
            }
        }
        // 判断最后一次记录
        ScriptHistory lastScriptHistory = scriptHistories.get(0);
        if (!lastScriptHistory.getScheduleRunnable()) {
            return Constant.JobState.FAILED;
        }
        if (Constant.JobState.UN_CONFIRMED_.equals(lastScriptHistory.getState()) || lastScriptHistory.isRunning()) {
            return Constant.JobState.RUNNING;
        }
        if (Constant.JobState.SUCCEEDED.equals(lastScriptHistory.getState())) {
            return Constant.JobState.SUCCEEDED;
        }
        return Constant.JobState.FAILED;
    }

}
