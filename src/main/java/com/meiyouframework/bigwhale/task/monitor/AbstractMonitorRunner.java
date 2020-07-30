package com.meiyouframework.bigwhale.task.monitor;

import com.meiyouframework.bigwhale.common.Constant;
import com.meiyouframework.bigwhale.common.pojo.HttpYarnApp;
import com.meiyouframework.bigwhale.task.AbstractNoticeableTask;
import com.meiyouframework.bigwhale.util.YarnApiUtils;
import com.meiyouframework.bigwhale.entity.*;
import com.meiyouframework.bigwhale.service.*;
import com.meiyouframework.bigwhale.service.auth.UserService;
import com.meiyouframework.bigwhale.task.cmd.CmdRecordRunner;
import com.meiyouframework.bigwhale.util.SchedulerUtils;
import com.meiyouframework.bigwhale.util.SpringContextUtils;
import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;

/**
 * @author meiyou big data group
 * @date 2020/01/08
 */
public abstract class AbstractMonitorRunner extends AbstractNoticeableTask implements InterruptableJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMonitorRunner.class);

    protected Monitor monitor;
    protected Script script;
    protected Cluster cluster;
    protected MonitorService monitorService;
    protected UserService userService;
    protected NoticeService noticeService;
    protected ScriptService scriptService;
    protected ClusterService clusterService;
    protected YarnAppService yarnAppService;
    protected Thread thread;
    protected volatile boolean interrupted = false;

    protected AbstractMonitorRunner() {
        monitorService = SpringContextUtils.getBean(MonitorService.class);
        userService = SpringContextUtils.getBean(UserService.class);
        noticeService = SpringContextUtils.getBean(NoticeService.class);
        scriptService = SpringContextUtils.getBean(ScriptService.class);
        clusterService = SpringContextUtils.getBean(ClusterService.class);
        yarnAppService = SpringContextUtils.getBean(YarnAppService.class);
    }

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
        String monitorInfoId = jobExecutionContext.getJobDetail().getKey().getName();
        monitor = monitorService.findById(monitorInfoId);
        if (monitor == null) {
            return;
        }
        monitor.setExecuteTime(new Date());
        monitorService.save(monitor);
        script = scriptService.findById(monitor.getScriptId());
        cluster = clusterService.findById(script.getClusterId());
        executeJob();
    }

    /**
     * 任务逻辑
     */
    public abstract void executeJob();

    protected YarnApp getYarnAppFromDatabase() {
        List<YarnApp> yarnApps = yarnAppService.findByQuery("scriptId=" + monitor.getScriptId());
        if (!CollectionUtils.isEmpty(yarnApps)) {
            return yarnApps.get(0);
        }
        return null;
    }

    protected YarnApp getYarnAppFromYarnServer() {
        HttpYarnApp httpYarnApp = YarnApiUtils.getActiveApp(cluster.getYarnUrl(), script.getUser(), script.getQueue(), script.getApp(), 3);
        if (httpYarnApp != null) {
            YarnApp yarnApp = new YarnApp();
            BeanUtils.copyProperties(httpYarnApp, yarnApp);
            yarnApp.setId(null);
            yarnApp.setUid(script.getUid());
            yarnApp.setScriptId(script.getId());
            yarnApp.setClusterId(script.getClusterId());
            yarnApp.setUpdateTime(new Date());
            yarnApp.setAppId(httpYarnApp.getId());
            yarnApp.setStartedTime(new Date(httpYarnApp.getStartedTime()));
            return yarnApp;
        }
        return null;
    }

    protected HttpYarnApp getLastNoActiveYarnApp() {
        return YarnApiUtils.getLastNoActiveApp(cluster.getYarnUrl(), script.getUser(), script.getQueue(), script.getApp(), 3);
    }

    /**
     * 重启
     * @return
     */
    protected boolean restart() {
        Script script = scriptService.findById(monitor.getScriptId());
        CmdRecordService cmdRecordService = SpringContextUtils.getBean(CmdRecordService.class);
        //检查是否存在当前脚本未执行或正在执行的任务
        CmdRecord cmdRecord = cmdRecordService.findOneByQuery("scriptId=" + monitor.getScriptId() + ";status=" + Constant.EXEC_STATUS_UNSTART + "," + Constant.EXEC_STATUS_DOING);
        if (cmdRecord != null) {
            return true;
        }
        CmdRecord record = CmdRecord.builder()
                .uid(monitor.getUid())
                .scriptId(script.getId())
                .createTime(new Date())
                .content(script.getScript())
                .timeout(script.getTimeout())
                .status(Constant.EXEC_STATUS_UNSTART)
                .agentId(script.getAgentId())
                .clusterId(script.getClusterId())
                .monitorId(monitor.getId())
                .build();
        record = cmdRecordService.save(record);
        try {
            CmdRecordRunner.build(record);
        } catch (SchedulerException e) {
            LOGGER.error("schedule submit error", e);
            return false;
        }
        return true;
    }

    public static void build(Monitor monitor) throws SchedulerException {
        if (monitor.getType() == Constant.MONITOR_TYPE_SPARK_STREAMING) {
            SchedulerUtils.scheduleCornJob(SparkMonitorRunner.class,
                    monitor.getId(),
                    Constant.JobGroup.MONITOR,
                    monitor.getCron());
        } else if (monitor.getType() == Constant.MONITOR_TYPE_FLINK_STREAMING) {
            SchedulerUtils.scheduleCornJob(FlinkMonitorRunner.class,
                    monitor.getId(),
                    Constant.JobGroup.MONITOR,
                    monitor.getCron());
        }
    }
}
