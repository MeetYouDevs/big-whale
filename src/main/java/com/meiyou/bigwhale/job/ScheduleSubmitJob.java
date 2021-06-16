package com.meiyou.bigwhale.job;

import com.meiyou.bigwhale.common.Constant;
import com.meiyou.bigwhale.entity.ScriptHistory;
import com.meiyou.bigwhale.service.ScriptHistoryService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Suxy
 * @date 2020/4/23
 * @description file description
 */
@DisallowConcurrentExecution
public class ScheduleSubmitJob implements Job {

    @Autowired
    private ScriptHistoryService scriptHistoryService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        List<ScriptHistory> scriptHistories = scriptHistoryService.findByQuery("state=" + Constant.JobState.WAITING_PARENT_,
                new Sort(Sort.Direction.ASC, "createTime"));
        for (ScriptHistory scriptHistory : scriptHistories) {
            String state;
            if (scriptHistory.getPreviousScheduleTopNodeId() == null) {
                state = Constant.JobState.INITED;
            } else {
                String previousNodeState = previousNodeState(scriptHistory.getScheduleId(), scriptHistory.getScheduleInstanceId(), scriptHistory.getPreviousScheduleTopNodeId());
                switch (previousNodeState) {
                    case Constant.JobState.SUCCEEDED:
                        state = Constant.JobState.INITED;
                        break;
                    case Constant.JobState.FAILED:
                        state = Constant.JobState.PARENT_FAILED_;
                        break;
                    case Constant.JobState.RUNNING:
                    default:
                        continue;
                }
            }
            scriptHistory.updateState(state);
            if (Constant.JobState.INITED.equals(state)) {
                scriptHistory = scriptHistoryService.save(scriptHistory);
                ScriptHistoryShellRunnerJob.build(scriptHistory);
            } else {
                scriptHistory.setFinishTime(new Date());
                scriptHistoryService.save(scriptHistory);
            }
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
                ";scheduleInstanceId=" + scheduleInstanceId);
        sort(scriptHistories);
        for (ScriptHistory scriptHistory : scriptHistories) {
            if (scriptHistory.isRunning()) {
                return Constant.JobState.RUNNING;
            }
            if (Constant.JobState.SUCCEEDED.equals(scriptHistory.getState())) {
                return Constant.JobState.SUCCEEDED;
            }
        }
        scriptHistories = scriptHistories.stream().filter(scriptHistory -> !scriptHistory.getScheduleSupplement()).collect(Collectors.toList());
        if (!scriptHistories.isEmpty()) {
            ScriptHistory scriptHistory = scriptHistories.get(0);
            String [] scheduleFailureHandleArr = scriptHistory.getScheduleFailureHandle().split(";");
            int failureRetries = Integer.parseInt(scheduleFailureHandleArr[0]);
            int currFailureRetries = Integer.parseInt(scheduleFailureHandleArr[2]);
            if (currFailureRetries < failureRetries) {
                // 一般失败后便会进入重试流程，如果最后一个任务结束30S之后还没有进入重试流程，则直接走失败判断流程
                if ((System.currentTimeMillis() - scriptHistory.getFinishTime().getTime()) < 30000) {
                    return Constant.JobState.RUNNING;
                }
            }
        }
        return Constant.JobState.FAILED;
    }

    private void sort(List<ScriptHistory> scriptHistories) {
        // 降序
        scriptHistories.sort((i1, i2) -> {
            if (i1.getId() > i2.getId()) {
                return -1;
            } else if (i1.getId() < i2.getId()) {
                return 1;
            }
            return 0;
        });
    }

}
