package com.meiyouframework.bigwhale.task.common;

import com.meiyouframework.bigwhale.common.Constant;
import com.meiyouframework.bigwhale.entity.Scheduling;
import com.meiyouframework.bigwhale.task.batch.DagTaskAppStatusUpdateJob;
import com.meiyouframework.bigwhale.util.MsgTools;
import com.meiyouframework.bigwhale.util.YarnApiUtils;
import com.meiyouframework.bigwhale.entity.Cluster;
import com.meiyouframework.bigwhale.entity.Script;
import com.meiyouframework.bigwhale.entity.YarnApp;
import com.meiyouframework.bigwhale.util.SchedulerUtils;
import com.meiyouframework.bigwhale.service.*;
import org.apache.commons.lang.time.DateUtils;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

/**
 * @author Suxy
 * @date 2019/10/15
 * @description file description
 */
@DisallowConcurrentExecution
public class PlatformTimeoutJob implements Job {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformTimeoutJob.class);

    @Autowired
    private NoticeService noticeService;
    @Autowired
    private SchedulingService schedulingService;
    @Autowired
    private YarnAppService yarnAppService;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private ScriptService scriptService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        List<JobExecutionContext> executionContexts;
        try {
            executionContexts = SchedulerUtils.getScheduler().getCurrentlyExecutingJobs();
        } catch (SchedulerException e) {
            LOGGER.error(e.getMessage(), e);
            return;
        }
        Date current = new Date();
        Date tenMinBefore = DateUtils.addMinutes(current, -10);
        executionContexts.forEach(executionContext -> {
            if (executionContext.getFireTime().before(tenMinBefore)) {
                JobKey jobKey = executionContext.getJobDetail().getKey();
                //yarn应用列表更新
                if (Constant.JobGroup.COMMON.equals(jobKey.getGroup())) {
                    if (RefreshActiveStateAppsJob.class.getSimpleName().equals(jobKey.getName())) {
                        String msg = MsgTools.getPlainErrorMsg(null, null, null, "调度平台-Yarn应用列表更新任务", "任务运行超时");
                        noticeService.sendDingding(new String[0], msg);
                        try {
                            SchedulerUtils.getScheduler().interrupt(jobKey);
                        } catch (UnableToInterruptJobException e) {
                            LOGGER.error(e.getMessage(), e);
                        }
                    }
                }
                //批处理应用状态更新
                if (Constant.JobGroup.BATCH.equals(jobKey.getGroup())) {
                    if (DagTaskAppStatusUpdateJob.class.getSimpleName().equals(jobKey.getName())) {
                        String msg = MsgTools.getPlainErrorMsg(null, null, null, "调度平台-批处理应用状态更新任务", "任务运行超时");
                        noticeService.sendDingding(new String[0], msg);
                        try {
                            SchedulerUtils.getScheduler().interrupt(jobKey);
                        } catch (UnableToInterruptJobException e) {
                            LOGGER.error(e.getMessage(), e);
                        }
                    }
                }
                //监控任务
                if (Constant.JobGroup.STREAMING.equals(jobKey.getGroup())) {
                    //杀掉应用
                    Scheduling scheduling = schedulingService.findById(jobKey.getName());
                    Script script = scriptService.findById(scheduling.getScriptIds());
                    YarnApp appInfo = yarnAppService.findOneByQuery("scriptId=" + scheduling.getScriptIds() + ";name=" + script.getApp());
                    if (appInfo != null) {
                        Cluster cluster = clusterService.findById(appInfo.getClusterId());
                        YarnApiUtils.killApp(cluster.getYarnUrl(), appInfo.getAppId());
                        yarnAppService.deleteById(appInfo.getId());
                    }
                    String msg = MsgTools.getPlainErrorMsg(null, null, null, "调度平台-监控任务（" + script.getName() + "）", "任务运行超时");
                    noticeService.sendDingding(new String[0], msg);
                    try {
                        SchedulerUtils.getScheduler().interrupt(jobKey);
                    } catch (UnableToInterruptJobException e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
            }
        });
    }
}
