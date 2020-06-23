package com.meiyouframework.bigwhale.task.common;

import com.meiyouframework.bigwhale.common.Constant;
import com.meiyouframework.bigwhale.common.pojo.HttpYarnApp;
import com.meiyouframework.bigwhale.entity.Cluster;
import com.meiyouframework.bigwhale.entity.CmdRecord;
import com.meiyouframework.bigwhale.entity.Script;
import com.meiyouframework.bigwhale.entity.YarnApp;
import com.meiyouframework.bigwhale.service.*;
import com.meiyouframework.bigwhale.task.AbstractCmdRecordTask;
import com.meiyouframework.bigwhale.util.SpringContextUtils;
import com.meiyouframework.bigwhale.util.YarnApiUtils;
import org.quartz.*;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;

/**
 * @author Suxy
 * @date 2020/4/23
 * @description file description
 */
@DisallowConcurrentExecution
public class CmdRecordAppStatusUpdateJob extends AbstractCmdRecordTask implements InterruptableJob {

    private Thread thread;
    private volatile boolean interrupted = false;

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
        CmdRecordService cmdRecordService = SpringContextUtils.getBean(CmdRecordService.class);
        Collection<CmdRecord> records = cmdRecordService.findByQuery("jobFinalStatus=UNDEFINED");
        if (CollectionUtils.isEmpty(records)) {
            return;
        }
        ScriptService scriptService = SpringContextUtils.getBean(ScriptService.class);
        ClusterService clusterService = SpringContextUtils.getBean(ClusterService.class);
        YarnAppService yarnAppService = SpringContextUtils.getBean(YarnAppService.class);
        for (CmdRecord cmdRecord : records) {
            Cluster cluster = clusterService.findById(cmdRecord.getClusterId());
            Script script = scriptService.findById(cmdRecord.getScriptId());
            List<YarnApp> yarnApps = yarnAppService.findByQuery("scriptId=" + cmdRecord.getScriptId());
            if (!yarnApps.isEmpty()) {
                continue;
            }
            HttpYarnApp httpYarnApp = YarnApiUtils.getActiveApp(cluster.getYarnUrl(), script.getUser(), script.getQueue(), script.getApp(), 3);
            if (httpYarnApp != null) {
                continue;
            }
            httpYarnApp = YarnApiUtils.getLastNoActiveApp(cluster.getYarnUrl(), script.getUser(), script.getQueue(), script.getApp(), 3);
            if (httpYarnApp != null) {
                String finalStatus = httpYarnApp.getFinalStatus();
                cmdRecord.setJobFinalStatus(finalStatus);
                if ("SUCCEEDED".equals(finalStatus)) {
                    //提交子任务
                    AgentService agentService = SpringContextUtils.getBean(AgentService.class);
                    submitSubCmdRecord(cmdRecord, cmdRecordService, agentService, scriptService);
                } else {
                    if (script.getType() == Constant.SCRIPT_TYPE_SPARK_BATCH) {
                        notice(cmdRecord, null, httpYarnApp.getId(), String.format(Constant.ERROR_TYPE_SPARK_BATCH_UNUSUAL, finalStatus));
                    } else {
                        notice(cmdRecord, null, httpYarnApp.getId(), String.format(Constant.ERROR_TYPE_FLINK_BATCH_UNUSUAL, finalStatus));
                    }
                }
            } else {
                cmdRecord.setJobFinalStatus("UNKNOWN");
            }
            cmdRecordService.save(cmdRecord);
        }
    }
}
