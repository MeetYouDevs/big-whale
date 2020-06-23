package com.meiyouframework.bigwhale.task.common;

import com.meiyouframework.bigwhale.common.Constant;
import com.meiyouframework.bigwhale.util.MsgTools;
import com.meiyouframework.bigwhale.util.YarnApiUtils;
import com.meiyouframework.bigwhale.entity.Cluster;
import com.meiyouframework.bigwhale.entity.Monitor;
import com.meiyouframework.bigwhale.entity.Script;
import com.meiyouframework.bigwhale.entity.YarnApp;
import com.meiyouframework.bigwhale.util.SchedulerUtils;
import com.meiyouframework.bigwhale.util.SpringContextUtils;
import com.meiyouframework.bigwhale.service.*;
import org.apache.commons.lang.time.DateUtils;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        NoticeService noticeService = SpringContextUtils.getBean(NoticeService.class);
        executionContexts.forEach(executionContext -> {
            if (executionContext.getFireTime().before(tenMinBefore)) {
                JobKey jobKey = executionContext.getJobDetail().getKey();
                //监控任务
                if (Constant.JobGroup.MONITOR.equals(jobKey.getGroup())) {
                    //杀掉应用
                    MonitorService monitorService = SpringContextUtils.getBean(MonitorService.class);
                    Monitor monitor = monitorService.findById(jobKey.getName());
                    YarnAppService yarnAppService = SpringContextUtils.getBean(YarnAppService.class);
                    YarnApp appInfo = yarnAppService.findOneByQuery("scriptId=" + monitor.getScriptId());
                    if (appInfo != null) {
                        ClusterService clusterService = SpringContextUtils.getBean(ClusterService.class);
                        Cluster cluster = clusterService.findById(appInfo.getClusterId());
                        YarnApiUtils.killApp(cluster.getYarnUrl(), appInfo.getAppId());
                        yarnAppService.deleteById(appInfo.getId());
                    }
                    ScriptService scriptService = SpringContextUtils.getBean(ScriptService.class);
                    Script script = scriptService.findById(monitor.getScriptId());
                    String msg = MsgTools.getPlainErrorMsg(null, null, null, "调度平台-监控任务（" + script.getName() + "）", "任务运行超时");
                    noticeService.sendDingding(new String[0], msg);
                    try {
                        SchedulerUtils.getScheduler().interrupt(jobKey);
                    } catch (UnableToInterruptJobException e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
                //yarn应用列表更新和离线应用状态更新
                if (Constant.JobGroup.COMMON.equals(jobKey.getGroup())) {
                    if (RefreshActiveStateAppsJob.class.getSimpleName().equals(jobKey.getName())) {
                        String msg = MsgTools.getPlainErrorMsg(null, null, null, "调度平台-Yarn应用列表更新任务", "任务运行超时");
                        noticeService.sendDingding(new String[0], msg);
                    }
                    if (CmdRecordAppStatusUpdateJob.class.getSimpleName().equals(jobKey.getName())) {
                        String msg = MsgTools.getPlainErrorMsg(null, null, null, "调度平台-离线应用状态更新任务", "任务运行超时");
                        noticeService.sendDingding(new String[0], msg);
                    }
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
