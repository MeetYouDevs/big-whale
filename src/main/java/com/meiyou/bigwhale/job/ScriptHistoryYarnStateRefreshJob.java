package com.meiyou.bigwhale.job;

import com.meiyou.bigwhale.common.Constant;
import com.meiyou.bigwhale.common.pojo.HttpYarnApp;
import com.meiyou.bigwhale.common.pojo.SchedulerInfo;
import com.meiyou.bigwhale.entity.Cluster;
import com.meiyou.bigwhale.entity.ScriptHistory;
import com.meiyou.bigwhale.service.ClusterService;
import com.meiyou.bigwhale.service.ScriptHistoryService;
import com.meiyou.bigwhale.util.YarnApiUtils;
import com.meiyou.bigwhale.util.YarnUtil;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

@DisallowConcurrentExecution
public class ScriptHistoryYarnStateRefreshJob extends AbstractRetryableJob implements InterruptableJob {

    private Thread thread;
    private volatile boolean interrupted = false;

    @Autowired
    private ScriptHistoryService scriptHistoryService;
    @Autowired
    private ClusterService clusterService;

    @Override
    public void interrupt() {
        if (!interrupted) {
            interrupted = true;
            thread.interrupt();
        }
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        thread = Thread.currentThread();
        // 遍历集群
        Iterable<Cluster> clusters = clusterService.findAll();
        for (Cluster cluster : clusters) {
            List<ScriptHistory> scriptHistories = scriptHistoryService.findByQuery("clusterId=" + cluster.getId() + ";jobFinalStatus=UNDEFINED");
            if (scriptHistories.isEmpty()) {
                continue;
            }
            // request Cluster Scheduler API for schedulerType
            SchedulerInfo scheduler = YarnApiUtils.getYarnSchedulerInfo(cluster.getYarnUrl());
            if (scheduler == null) {
                continue;
            }
            // 请求yarn web url, 获取所有应用
            List<HttpYarnApp> httpYarnApps = YarnApiUtils.getActiveApps(cluster.getYarnUrl());
            // 请求出错，不清理数据
            if (httpYarnApps == null) {
                continue;
            }
            String schedulerType = scheduler.getType();
            httpYarnApps.removeIf(httpYarnApp -> !httpYarnApp.getName().contains(".bw_instance_") && !httpYarnApp.getName().contains(".bw_test_instance_"));
            Map<String, ScriptHistory> yarnParamsToScriptHistoryMap = new HashMap<>();
            scriptHistories.forEach(scriptHistory -> {
                String [] jobParams = scriptHistory.getJobParams().split(";");
                String user = jobParams[0];
                String queue = YarnUtil.getQueueName(schedulerType, jobParams[1]);
                String app = jobParams[2];
                String key = user + ";" + queue + ";" + app;
                yarnParamsToScriptHistoryMap.put(key, scriptHistory);
            });
            Set<Integer> matchIds = new HashSet<>();
            for (HttpYarnApp httpYarnApp : httpYarnApps) {
                String key = httpYarnApp.getUser() + ";" + httpYarnApp.getQueue() + ";" + httpYarnApp.getName();
                if (!yarnParamsToScriptHistoryMap.containsKey(key)) {
                    continue;
                }
                ScriptHistory scriptHistory = yarnParamsToScriptHistoryMap.get(key);
                if (key.contains(".bw_test_instance_")) {
                    YarnApiUtils.killApp(cluster.getYarnUrl(), httpYarnApp.getId());
                    scriptHistory.updateState(Constant.JobState.SUCCEEDED);
                    scriptHistory.setJobFinalStatus(Constant.JobState.KILLED);
                    scriptHistory.setFinishTime(new Date());
                    scriptHistoryService.save(scriptHistory);
                } else {
                    updateMatchScriptHistory(httpYarnApp, scriptHistory);
                }
                matchIds.add(scriptHistory.getId());
            }
            scriptHistories.forEach(scriptHistory -> {
                if (!matchIds.contains(scriptHistory.getId())) {
                    updateNoMatchScriptHistory(cluster.getYarnUrl(), scriptHistory, schedulerType);
                }
            });
        }
    }

    private void updateMatchScriptHistory(HttpYarnApp httpYarnApp, ScriptHistory scriptHistory) {
        if ("RUNNING".equals(httpYarnApp.getState()) ||
                "FINAL_SAVING".equals(httpYarnApp.getState()) ||
                "FINISHING".equals(httpYarnApp.getState()) ||
                "KILLING".equals(httpYarnApp.getState())) {
            scriptHistory.updateState(Constant.JobState.ACCEPTED);
            scriptHistory.updateState(Constant.JobState.RUNNING);
        } else {
            scriptHistory.updateState(httpYarnApp.getState());
        }
        scriptHistory.setJobId(httpYarnApp.getId());
        scriptHistory.setJobUrl(httpYarnApp.getTrackingUrl());
        scriptHistory.setStartTime(new Date(httpYarnApp.getStartedTime()));
        scriptHistoryService.save(scriptHistory);
    }

    private void updateNoMatchScriptHistory(String yarnUrl, ScriptHistory scriptHistory, String schedulerType) {
        String [] jobParams = scriptHistory.getJobParams().split(";");
        HttpYarnApp httpYarnApp = YarnApiUtils.getLastNoActiveApp(yarnUrl, schedulerType, jobParams[0], jobParams[1], jobParams[2], 3);
        if (httpYarnApp != null) {
            if ("FINISHED".equals(httpYarnApp.getState())) {
                scriptHistory.updateState(httpYarnApp.getFinalStatus());
            } else {
                scriptHistory.updateState(httpYarnApp.getState());
            }
            scriptHistory.setJobId(httpYarnApp.getId());
            scriptHistory.setJobUrl(httpYarnApp.getTrackingUrl());
            scriptHistory.setJobFinalStatus(httpYarnApp.getFinalStatus());
            if ("FAILED".equals(httpYarnApp.getFinalStatus())) {
                if (httpYarnApp.getDiagnostics() != null) {
                    if (httpYarnApp.getDiagnostics().length() > 61440) {
                        scriptHistory.setErrors(httpYarnApp.getDiagnostics().substring(0, 61440));
                    } else {
                        scriptHistory.setErrors(httpYarnApp.getDiagnostics());
                    }
                }
            }
            scriptHistory.setStartTime(new Date(httpYarnApp.getStartedTime()));
            scriptHistory.setFinishTime(new Date(httpYarnApp.getFinishedTime()));
        } else {
            scriptHistory.updateState(Constant.JobState.FAILED);
            scriptHistory.setJobFinalStatus("UNKNOWN");
            scriptHistory.setFinishTime(new Date());
        }
        scriptHistoryService.save(scriptHistory);
        if (!"SUCCEEDED".equals(scriptHistory.getJobFinalStatus())) {
            if (Constant.ScriptType.SPARK_BATCH.equals(scriptHistory.getScriptType())) {
                retryCurrentNode(scriptHistory, String.format(Constant.ErrorType.SPARK_BATCH_UNUSUAL, scriptHistory.getJobFinalStatus()));
            } else if (Constant.ScriptType.FLINK_BATCH.equals(scriptHistory.getScriptType())) {
                retryCurrentNode(scriptHistory, String.format(Constant.ErrorType.FLINK_BATCH_UNUSUAL, scriptHistory.getJobFinalStatus()));
            }
        }
    }

}
