package com.meiyou.bigwhale.scheduler.system;

import com.meiyou.bigwhale.common.Constant;
import com.meiyou.bigwhale.entity.*;
import com.meiyou.bigwhale.scheduler.AbstractNoticeable;
import com.meiyou.bigwhale.scheduler.ScriptJobYarnStateRefresher;
import com.meiyou.bigwhale.service.*;
import com.meiyou.bigwhale.util.YarnApiUtils;
import com.meiyou.bigwhale.util.SchedulerUtils;
import org.apache.commons.lang.time.DateUtils;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

/**
 * @author Suxy
 * @date 2019/10/15
 * @description file description
 */
@DisallowConcurrentExecution
public class PlatformTimeoutChecker extends AbstractNoticeable implements Job {

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
            e.printStackTrace();
            return;
        }
        Date current = new Date();
        Date tenMinBefore = DateUtils.addMinutes(current, -10);
        executionContexts.forEach(executionContext -> {
            if (executionContext.getFireTime().before(tenMinBefore)) {
                JobKey jobKey = executionContext.getJobDetail().getKey();
                //yarn应用列表更新
                if (Constant.JobGroup.COMMON.equals(jobKey.getGroup())) {
                    if (ActiveYarnAppRefresher.class.getSimpleName().equals(jobKey.getName())) {
                        notice("系统任务-Yarn应用列表更新", "系统任务运行超时");
                        SchedulerUtils.interrupt(jobKey.getName(), jobKey.getGroup());
                    }
                    if (ScriptJobYarnStateRefresher.class.getSimpleName().equals(jobKey.getName())) {
                        notice("系统任务-Yarn应用状态更新", "系统任务运行超时");
                        SchedulerUtils.interrupt(jobKey.getName(), jobKey.getGroup());
                    }
                }
                //监控任务
                if (Constant.JobGroup.MONITOR.equals(jobKey.getGroup())) {
                    //杀掉应用
                    Monitor monitor = monitorService.findById(Integer.parseInt(jobKey.getName()));
                    Script script = scriptService.findOneByQuery("monitorId=" + monitor.getId());
                    ScriptHistory scriptHistory = scriptHistoryService.findScriptLatest(script.getId());
                    if (scriptHistory.getJobId() != null) {
                        Cluster cluster = clusterService.findById(scriptHistory.getClusterId());
                        boolean success = YarnApiUtils.killApp(cluster.getYarnUrl(), scriptHistory.getJobId());
                        if (success) {
                            yarnAppService.deleteByQuery("clusterId=" + scriptHistory.getClusterId() + ";appId=" + scriptHistory.getJobId());
                        }
                    }
                    notice("实时任务【" + script.getName() + "】监控", "监控任务运行超时");
                    SchedulerUtils.interrupt(jobKey.getName(), jobKey.getGroup());
                }
            }
        });
    }
}
