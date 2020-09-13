package com.meiyouframework.bigwhale.task.timed;

import com.alibaba.fastjson.JSON;
import com.meiyouframework.bigwhale.common.Constant;
import com.meiyouframework.bigwhale.task.cmd.CmdRecordRunner;
import com.meiyouframework.bigwhale.util.SchedulerUtils;
import com.meiyouframework.bigwhale.entity.*;
import com.meiyouframework.bigwhale.service.*;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Suxy
 * @date 2019/9/5
 * @description file description
 */
@DisallowConcurrentExecution
public class TimedTask implements Job {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimedTask.class);

    private DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    @Autowired
    private CmdRecordService cmdRecordService;
    @Autowired
    private ScriptService scriptService;
    @Autowired
    private SchedulingService schedulingService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        String schedulingId = jobExecutionContext.getJobDetail().getKey().getName();
        Scheduling scheduling = schedulingService.findById(schedulingId);
        if (scheduling.getLastExecuteTime() != null && !scheduling.getRepeatSubmit()) {
            if (!isLastTimeCompleted(scheduling)) {
                return;
            }
        }
        String now = dateFormat.format(new Date());
        try {
            scheduling.setLastExecuteTime(dateFormat.parse(now));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        schedulingService.save(scheduling);
        Map<String, String> nodeIdToScriptId = scheduling.analyzeNextNode(null);
        nodeIdToScriptId.forEach((nodeId, scriptId) -> {
            Script script = scriptService.findById(scriptId);
            CmdRecord cmdRecord = CmdRecord.builder()
                    .uid(scheduling.getUid())
                    .scriptId(scriptId)
                    .status(Constant.EXEC_STATUS_UNSTART)
                    .agentId(script.getAgentId())
                    .clusterId(script.getClusterId())
                    .schedulingId(scheduling.getId())
                    .schedulingInstanceId(now)
                    .schedulingNodeId(nodeId)
                    .content(script.getScript())
                    .timeout(script.getTimeout())
                    .createTime(new Date())
                    .build();
            if (!jobExecutionContext.getMergedJobDataMap().isEmpty()) {
                cmdRecord.setArgs(JSON.toJSONString(jobExecutionContext.getMergedJobDataMap()));
            }
            cmdRecord = cmdRecordService.save(cmdRecord);
            //提交任务
            try {
                CmdRecordRunner.build(cmdRecord);
            } catch (SchedulerException e) {
                LOGGER.error("schedule submit error", e);
            }
        });
    }

    private boolean isLastTimeCompleted(Scheduling scheduling) {
        List<CmdRecord> cmdRecords = cmdRecordService.findByQuery(
                ";schedulingId=" + scheduling.getId() +
                        ";schedulingInstanceId=" +  dateFormat.format(scheduling.getLastExecuteTime()),
                Sort.by(Sort.Direction.ASC, "createTime"));
        //非程序意外退出的情况下，相关执行记录不会为空
        if (cmdRecords.isEmpty()) {
            return true;
        }
        for (CmdRecord cmdRecord : cmdRecords) {
            if (cmdRecord.getStatus() == Constant.EXEC_STATUS_UNSTART || cmdRecord.getStatus() == Constant.EXEC_STATUS_DOING) {
                return false;
            }
        }
        if (cmdRecords.size() == scheduling.getScriptIds().split(",").length) {
            for (CmdRecord cmdRecord : cmdRecords) {
                Script script = scriptService.findById(cmdRecord.getScriptId());
                if (isYarnBatch(script)) {
                    if ("UNDEFINED".equals(cmdRecord.getJobFinalStatus())) {
                        return false;
                    }
                }
            }
            return true;
        } else {
            List<CmdRecord> lastCmdRecords = new ArrayList<>();
            filterNodeTreeLast(scheduling, cmdRecords, lastCmdRecords, null);
            for (CmdRecord lastCmdRecord : lastCmdRecords) {
                Script script = scriptService.findById(lastCmdRecord.getScriptId());
                if (isYarnBatch(script)) {
                    if ("UNDEFINED".equals(lastCmdRecord.getJobFinalStatus()) || "SUCCEEDED".equals(lastCmdRecord.getJobFinalStatus())) {
                        return false;
                    }
                } else {
                    if (Constant.EXEC_STATUS_FINISH == lastCmdRecord.getStatus())  {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    private void filterNodeTreeLast(Scheduling scheduling, List<CmdRecord> cmdRecords, List<CmdRecord> lastCmdRecords, String currentNodeId) {
        Map<String, String> nodeIdToScriptId = scheduling.analyzeNextNode(currentNodeId);
        for (Map.Entry<String, String> entry : nodeIdToScriptId.entrySet()) {
            CmdRecord currentCmdRecord = null;
            boolean match = false;
            for (CmdRecord cmdRecord : cmdRecords) {
                if (cmdRecord.getSchedulingNodeId().equals(currentNodeId)) {
                    currentCmdRecord = cmdRecord;
                }
                if (cmdRecord.getSchedulingNodeId().equals(entry.getKey())) {
                    match = true;
                }
            }
            if (match) {
                filterNodeTreeLast(scheduling, cmdRecords, lastCmdRecords, entry.getKey());
            } else {
                if (currentCmdRecord != null) {
                    lastCmdRecords.add(currentCmdRecord);
                }
            }
        }
    }

    private boolean isYarnBatch(Script script) {
        return script.getType() == Constant.SCRIPT_TYPE_SPARK_BATCH || script.getType() == Constant.SCRIPT_TYPE_FLINK_BATCH;
    }

    public static void build(Scheduling scheduling) throws SchedulerException {
        SchedulerUtils.scheduleCornJob(TimedTask.class,
                scheduling.getId(),
                Constant.JobGroup.TIMED,
                scheduling.generateCron(),
                null,
                scheduling.getStartTime(),
                scheduling.getEndTime());
    }

}
