package com.meiyou.bigwhale.job;

import com.meiyou.bigwhale.common.Constant;
import com.meiyou.bigwhale.common.pojo.HttpYarnApp;
import com.meiyou.bigwhale.common.pojo.SchedulerInfo;
import com.meiyou.bigwhale.entity.Cluster;
import com.meiyou.bigwhale.entity.ScriptHistory;
import com.meiyou.bigwhale.service.ClusterService;
import com.meiyou.bigwhale.service.ScriptHistoryService;
import com.meiyou.bigwhale.util.SchedulerUtils;
import com.meiyou.bigwhale.util.YarnApiUtils;
import org.apache.commons.lang.StringUtils;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private ScriptHistoryService scriptHistoryService;
    @Autowired
    private ClusterService clusterService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        //未开始执行和正在执行的
        List<ScriptHistory> scriptHistories = scriptHistoryService.findByQuery("state=" + StringUtils.join(RUNNING_STATES, ",") + ";jobFinalStatus-");
        if (CollectionUtils.isEmpty(scriptHistories)) {
            return;
        }
        for (ScriptHistory scriptHistory : scriptHistories) {
            if (scriptHistoryService.execTimeout(scriptHistory)) {
                boolean retry = true;
                // Yarn资源不够时，客户端会长时间处于提交请求状态，平台无法中断此请求，故在此处再判断一次状态
                if (scriptHistory.getClusterId() != null && scriptHistory.getState().equals(Constant.JobState.SUBMITTING)) {
                    Cluster cluster = clusterService.findById(scriptHistory.getClusterId());
                    // request Cluster Scheduler API for schedulerType
                    SchedulerInfo scheduler = YarnApiUtils.getYarnSchedulerInfo(cluster.getYarnUrl());
                    if (scheduler == null) {
                        // handle this next time.
                        continue;
                    }
                    String schedulerType = scheduler.getType();
                    String [] jobParams = scriptHistory.getJobParams().split(";");
                    HttpYarnApp httpYarnApp = YarnApiUtils.getActiveApp(cluster.getYarnUrl(), schedulerType, jobParams[0], jobParams[1], jobParams[2], 3);
                    if (httpYarnApp != null) {
                        retry = false;
                        scriptHistory.updateState(Constant.JobState.SUBMITTED);
                        scriptHistory.setJobFinalStatus("UNDEFINED");
                    } else {
                        scriptHistory.updateState(Constant.JobState.TIMEOUT);
                        scriptHistory.setFinishTime(new Date());
                    }
                } else {
                    scriptHistory.updateState(Constant.JobState.TIMEOUT);
                    scriptHistory.setFinishTime(new Date());
                }
                scriptHistoryService.save(scriptHistory);
                // 处理调度
                SchedulerUtils.interrupt(scriptHistory.getId(), Constant.JobGroup.SCRIPT_HISTORY);
                SchedulerUtils.deleteJob(scriptHistory.getId(), Constant.JobGroup.SCRIPT_HISTORY);
                // 重试
                if (retry) {
                    retryCurrentNode(scriptHistory, Constant.ErrorType.TIMEOUT);
                }
            }
        }
    }

}
