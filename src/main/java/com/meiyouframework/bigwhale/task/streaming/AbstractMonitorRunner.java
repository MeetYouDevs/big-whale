package com.meiyouframework.bigwhale.task.streaming;

import com.meiyouframework.bigwhale.common.Constant;
import com.meiyouframework.bigwhale.common.pojo.HttpYarnApp;
import com.meiyouframework.bigwhale.task.AbstractNoticeableTask;
import com.meiyouframework.bigwhale.util.SpringContextUtils;
import com.meiyouframework.bigwhale.util.YarnApiUtils;
import com.meiyouframework.bigwhale.entity.*;
import com.meiyouframework.bigwhale.service.*;
import com.meiyouframework.bigwhale.task.cmd.CmdRecordRunner;
import com.meiyouframework.bigwhale.util.SchedulerUtils;
import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

import java.util.Date;

/**
 * @author meiyou big data group
 * @date 2020/01/08
 */
public abstract class AbstractMonitorRunner extends AbstractNoticeableTask implements InterruptableJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMonitorRunner.class);

    protected Scheduling scheduling;
    protected Script script;
    protected Cluster cluster;
    private Thread thread;
    private volatile boolean interrupted = false;

    @Autowired
    private SchedulingService schedulingService;
    @Autowired
    private ScriptService scriptService;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private YarnAppService yarnAppService;
    @Autowired
    private CmdRecordService cmdRecordService;

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
        scheduling = schedulingService.findById(monitorInfoId);
        if (scheduling == null) {
            return;
        }
        scheduling.setLastExecuteTime(new Date());
        schedulingService.save(scheduling);
        script = scriptService.findById(scheduling.getScriptIds());
        cluster = clusterService.findById(script.getClusterId());
        executeJob();
    }

    /**
     * 任务逻辑
     */
    public abstract void executeJob();

    protected YarnApp getYarnAppFromDatabase() {
        return yarnAppService.findOneByQuery("scriptId=" + scheduling.getScriptIds() + ";name=" + script.getApp());
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
    protected boolean restart(String currentJobFinalStatus) {
        Script script = scriptService.findById(scheduling.getScriptIds());
        //检查是否存在当前脚本未执行或正在执行的任务
        CmdRecord cmdRecord = cmdRecordService.findOneByQuery("scriptId=" + scheduling.getScriptIds() + ";status=" + Constant.EXEC_STATUS_UNSTART + "," + Constant.EXEC_STATUS_DOING);
        if (cmdRecord != null) {
            return true;
        }
        //更新应用状态
        cmdRecord = cmdRecordService.findOneByQuery(
                ";scriptId=" + scheduling.getScriptIds() + ";jobFinalStatus=UNDEFINED",
                Sort.by(Sort.Direction.DESC, "createTime"));
        if (cmdRecord != null) {
            cmdRecord.setJobFinalStatus(currentJobFinalStatus);
            cmdRecordService.save(cmdRecord);
        }
        CmdRecord record = CmdRecord.builder()
                .uid(scheduling.getUid())
                .scriptId(script.getId())
                .status(Constant.EXEC_STATUS_UNSTART)
                .agentId(script.getAgentId())
                .clusterId(script.getClusterId())
                .schedulingId(scheduling.getId())
                .content(script.getScript())
                .timeout(script.getTimeout())
                .createTime(new Date())
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

    public static void build(Scheduling scheduling) throws SchedulerException {
        Script script = SpringContextUtils.getBean(ScriptService.class).findById(scheduling.getScriptIds());
        if (script.getType() == Constant.SCRIPT_TYPE_SPARK_STREAMING) {
            SchedulerUtils.scheduleCornJob(SparkMonitorRunner.class,
                    scheduling.getId(),
                    Constant.JobGroup.STREAMING,
                    scheduling.generateCron(),
                    null,
                    scheduling.getStartTime(),
                    scheduling.getEndTime());
        } else if (script.getType() == Constant.SCRIPT_TYPE_FLINK_STREAMING) {
            SchedulerUtils.scheduleCornJob(FlinkMonitorRunner.class,
                    scheduling.getId(),
                    Constant.JobGroup.STREAMING,
                    scheduling.generateCron(),
                    null,
                    scheduling.getStartTime(),
                    scheduling.getEndTime());
        }
    }
}
