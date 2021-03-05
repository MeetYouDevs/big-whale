package com.meiyou.bigwhale.job;

import com.meiyou.bigwhale.common.Constant;
import com.meiyou.bigwhale.entity.ScriptHistory;
import com.meiyou.bigwhale.util.SchedulerUtils;
import org.apache.commons.lang.StringUtils;
import org.quartz.*;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @author Suxy
 * @date 2019/9/6
 * @description file description
 */
@DisallowConcurrentExecution
public class ScriptHistoryTimeoutJob extends AbstractRetryableJob implements Job {

    private static final String [] RUNNING_STATES = new String[] {
            Constant.JobState.INITED,
            Constant.JobState.SUBMITTING,
            Constant.JobState.SUBMITTED,
            Constant.JobState.ACCEPTED,
            Constant.JobState.RUNNING
    };

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        //未开始执行和正在执行的
        List<ScriptHistory> scriptHistories = scriptHistoryService.findByQuery("state=" + StringUtils.join(RUNNING_STATES, ",") + ";jobFinalStatus-");
        if (CollectionUtils.isEmpty(scriptHistories)) {
            return;
        }
        for (ScriptHistory scriptHistory : scriptHistories) {
            if (scriptHistoryService.execTimeout(scriptHistory)) {
                if (scriptHistory.getSteps().contains(Constant.JobState.SUBMITTED)) {
                    scriptHistory.updateState(Constant.JobState.TIMEOUT);
                } else {
                    scriptHistory.updateState(Constant.JobState.SUBMITTING_TIMEOUT);
                }
                scriptHistory.setFinishTime(new Date());
                scriptHistoryService.save(scriptHistory);
                // 处理调度
                SchedulerUtils.interrupt(scriptHistory.getId(), Constant.JobGroup.SCRIPT_HISTORY);
                SchedulerUtils.deleteJob(scriptHistory.getId(), Constant.JobGroup.SCRIPT_HISTORY);
                // 重试
                retryCurrentNode(scriptHistory, Constant.ErrorType.TIMEOUT);
            }
        }
    }

}
