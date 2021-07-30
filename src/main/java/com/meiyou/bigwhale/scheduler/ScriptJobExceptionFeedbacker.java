package com.meiyou.bigwhale.scheduler;

import com.meiyou.bigwhale.common.Constant;
import com.meiyou.bigwhale.common.pojo.HttpYarnApp;
import com.meiyou.bigwhale.entity.Cluster;
import com.meiyou.bigwhale.entity.ScriptHistory;
import com.meiyou.bigwhale.scheduler.job.ScriptJob;
import com.meiyou.bigwhale.service.ClusterService;
import com.meiyou.bigwhale.service.ScriptHistoryService;
import com.meiyou.bigwhale.util.SchedulerUtils;
import com.meiyou.bigwhale.util.YarnApiUtils;
import org.apache.commons.lang.StringUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

/**
 * @author Suxy
 * @date 2019/9/6
 * @description file description
 */
@DisallowConcurrentExecution
public class ScriptJobExceptionFeedbacker extends AbstractRetryable implements Job {

    private String[] feedbackStates = new String[] {
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
        List<ScriptHistory> scriptHistories = scriptHistoryService.findByQuery("state=" + Constant.JobState.SUBMIT_WAIT);
        if (!scriptHistories.isEmpty()) {
            for (ScriptHistory scriptHistory : scriptHistories) {
                scriptHistory.updateState(Constant.JobState.SUBMITTING);
                scriptHistory = scriptHistoryService.save(scriptHistory);
                ScriptJob.build(scriptHistory);
            }
        }
        scriptHistories = scriptHistoryService.findByQuery("state=" + StringUtils.join(feedbackStates, ",") + ";jobFinalStatus-");
        if (scriptHistories.isEmpty()) {
            return;
        }
        for (ScriptHistory scriptHistory : scriptHistories) {
            if (!SchedulerUtils.checkExists(scriptHistory.getId(), Constant.JobGroup.SCRIPT_JOB)) {
                // 服务意外退出的情况下，期间的任务状态更新可能丢失，
                // 针对Yarn类型的任务，符合条件的情况下扔给ScriptJobYarnStateRefresher检查
                if (Constant.ScriptType.SPARK_BATCH.equals(scriptHistory.getScriptType()) || Constant.ScriptType.SPARK_STREAM.equals(scriptHistory.getScriptType()) ||
                        Constant.ScriptType.FLINK_BATCH.equals(scriptHistory.getScriptType()) || Constant.ScriptType.FLINK_STREAM.equals(scriptHistory.getScriptType())) {
                    Cluster cluster = clusterService.findById(scriptHistory.getClusterId());
                    String[] params = scriptHistory.getJobParams().split(";");
                    HttpYarnApp httpYarnApp = YarnApiUtils.getActiveApp(cluster.getYarnUrl(), params[0], params[1], params[2], 3);
                    if (httpYarnApp == null) {
                        httpYarnApp = YarnApiUtils.getLastNoActiveApp(cluster.getYarnUrl(), params[0], params[1], params[2], 3);
                    }
                    if (httpYarnApp != null) {
                        scriptHistory.updateState(Constant.JobState.SUBMITTED);
                        scriptHistory.updateState(Constant.JobState.ACCEPTED);
                        scriptHistory.updateState(Constant.JobState.RUNNING);
                        scriptHistory.setJobFinalStatus("UNDEFINED");
                        scriptHistoryService.save(scriptHistory);
                        continue;
                    }
                }
                scriptHistory.updateState(Constant.JobState.FAILED);
                scriptHistory.setErrors(scriptHistory.getErrors() != null ? scriptHistory.getErrors() + "\nServer unexpected exit" : "Server unexpected exit");
                scriptHistory.setFinishTime(new Date());
                scriptHistory = scriptHistoryService.save(scriptHistory);
                retryCurrentNode(scriptHistory, Constant.ErrorType.SERVER_UNEXPECTED_EXIT);
            }
        }
    }

}
