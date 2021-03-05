package com.meiyou.bigwhale.job.system;

import com.meiyou.bigwhale.common.Constant;
import com.meiyou.bigwhale.entity.*;
import com.meiyou.bigwhale.job.AbstractNoticeableJob;
import com.meiyou.bigwhale.job.ScriptHistoryYarnStateRefreshJob;
import com.meiyou.bigwhale.service.*;
import com.meiyou.bigwhale.util.YarnApiUtils;
import com.meiyou.bigwhale.util.SchedulerUtils;
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
public class PlatformTimeoutJob extends AbstractNoticeableJob implements Job {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformTimeoutJob.class);

    @Autowired
    private MonitorService monitorService;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private ScriptService scriptService;
    @Autowired
    private ScriptHistoryService scriptHistoryService;
    @Autowired
    private YarnAppService yarnAppService;

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
                    if (ActiveYarnAppRefreshJob.class.getSimpleName().equals(jobKey.getName())) {
                        notice("调度平台-Yarn应用列表更新任务", "系统任务运行超时");
                        SchedulerUtils.interrupt(jobKey.getName(), jobKey.getGroup());
                    }
                    if (ScriptHistoryYarnStateRefreshJob.class.getSimpleName().equals(jobKey.getName())) {
                        notice("调度平台-Yarn应用状态更新任务", "系统任务运行超时");
                        SchedulerUtils.interrupt(jobKey.getName(), jobKey.getGroup());
                    }
                }
                //监控任务
                if (Constant.JobGroup.MONITOR.equals(jobKey.getGroup())) {
                    //杀掉应用
                    Monitor monitor = monitorService.findById(Integer.parseInt(jobKey.getName()));
                    Script script = scriptService.findOneByQuery("monitorId=" + monitor.getId());
                    ScriptHistory scriptHistory = scriptHistoryService.findNoScheduleLatestByScriptId(script.getId());
                    if (scriptHistory.getJobId() != null) {
                        Cluster cluster = clusterService.findById(scriptHistory.getClusterId());
                        YarnApiUtils.killApp(cluster.getYarnUrl(), scriptHistory.getJobId());
                        yarnAppService.deleteByQuery("clusterId=" + scriptHistory.getClusterId() + ";appId=" + scriptHistory.getJobId());
                    }
                    notice("调度平台-实时任务【" + script.getName() + "】监控", "系统任务运行超时");
                    SchedulerUtils.interrupt(jobKey.getName(), jobKey.getGroup());
                }
            }
        });
    }
}
