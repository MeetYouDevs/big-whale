package com.meiyouframework.bigwhale.task.timed;

import com.alibaba.fastjson.JSON;
import com.meiyouframework.bigwhale.common.Constant;
import com.meiyouframework.bigwhale.task.cmd.CmdRecordRunner;
import com.meiyouframework.bigwhale.util.SchedulerUtils;
import com.meiyouframework.bigwhale.util.SpringContextUtils;
import com.meiyouframework.bigwhale.entity.*;
import com.meiyouframework.bigwhale.service.*;
import org.apache.commons.lang.StringUtils;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private CmdRecordService cmdRecordService;
    private ScriptService scriptService;

    public TimedTask() {
        cmdRecordService = SpringContextUtils.getBean(CmdRecordService.class);
        scriptService = SpringContextUtils.getBean(ScriptService.class);
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        String schedulingId = jobExecutionContext.getJobDetail().getKey().getName();
        SchedulingService schedulingService = SpringContextUtils.getBean(SchedulingService.class);
        AgentService agentService = SpringContextUtils.getBean(AgentService.class);
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
            String agentId = script.getAgentId();
            if (StringUtils.isBlank(agentId)) {
                Agent agent = agentService.getByClusterId(script.getClusterId());
                if (agent != null) {
                    agentId = agent.getId();
                }
            }
            CmdRecord cmdRecord = CmdRecord.builder()
                    .uid(scheduling.getUid())
                    .scriptId(scriptId)
                    .createTime(new Date())
                    .content(script.getScript())
                    .timeout(script.getTimeout())
                    .status(Constant.EXEC_STATUS_UNSTART)
                    .agentId(agentId)
                    .clusterId(script.getClusterId())
                    .schedulingId(scheduling.getId())
                    .schedulingInstanceId(now)
                    .schedulingNodeId(nodeId)
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
        Date startDate = scheduling.getStartTime();
        Date endDate = scheduling.getEndTime();
        if (new Date().after(endDate)) {
            return;
        }
        if (StringUtils.isNotBlank(scheduling.getCron())) {
            SchedulerUtils.scheduleCornJob(TimedTask.class, scheduling.getId(), Constant.JobGroup.TIMED, scheduling.getCron(), null, startDate, endDate);
        } else {
            String cron = null;
            if (scheduling.getCycle() == Constant.TIMER_CYCLE_MINUTE) {
                cron = "0 */" + scheduling.getIntervals() + " * * * ? *";
            } else if (scheduling.getCycle() == Constant.TIMER_CYCLE_HOUR) {
                cron = "0 " + scheduling.getMinute() + " * * * ? *";
            } else if (scheduling.getCycle() == Constant.TIMER_CYCLE_DAY) {
                cron = "0 " + scheduling.getMinute() + " " + scheduling.getHour() + " * * ? *";
            } else if (scheduling.getCycle() == Constant.TIMER_CYCLE_WEEK) {
                cron = "0 " + scheduling.getMinute() + " " + scheduling.getHour() + " ? * " + scheduling.getWeek() + " *";
            }
            if (cron == null) {
                throw new SchedulerException("cron expression is incorrect");
            }
            SchedulerUtils.scheduleCornJob(TimedTask.class, scheduling.getId(), Constant.JobGroup.TIMED, cron, null, startDate, endDate);
        }
    }

}
