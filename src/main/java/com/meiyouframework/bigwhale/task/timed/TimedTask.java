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
import java.util.Date;
import java.util.List;

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
        Script script = scriptService.findById(scheduling.getScriptId());
        String agentId = script.getAgentId();
        if (StringUtils.isBlank(agentId)) {
            Agent agent = agentService.getByClusterId(script.getClusterId());
            if (agent != null) {
                agentId = agent.getId();
            }
        }
        CmdRecord cmdRecord = CmdRecord.builder()
                .uid(scheduling.getUid())
                .scriptId(script.getId())
                .subScriptIds(scheduling.getSubScriptIds())
                .createTime(new Date())
                .content(script.getScript())
                .timeout(script.getTimeout())
                .status(Constant.EXEC_STATUS_UNSTART)
                .agentId(agentId)
                .clusterId(script.getClusterId())
                .schedulingId(scheduling.getId())
                .schedulingInstanceId(now)
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
    }

    private boolean isLastTimeCompleted(Scheduling scheduling) {
        List<CmdRecord> cmdRecords = cmdRecordService.findByQuery(
                ";schedulingId=" + scheduling.getId() +
                        ";schedulingInstanceId=" +  dateFormat.format(scheduling.getLastExecuteTime()),
                Sort.by(Sort.Direction.DESC, "createTime"));
        //非程序意外退出的情况下，相关执行记录不会为空
        if (cmdRecords.isEmpty()) {
            return true;
        }
        for (CmdRecord cmdRecord : cmdRecords) {
            if (cmdRecord.getStatus() == Constant.EXEC_STATUS_UNSTART || cmdRecord.getStatus() == Constant.EXEC_STATUS_DOING) {
                return false;
            }
        }
        CmdRecord lastCmdRecord = cmdRecords.get(0);
        Script script = scriptService.findById(lastCmdRecord.getScriptId());
        if (StringUtils.isBlank(scheduling.getSubScriptIds())) {
            if (isYarnBatch(script)) {
                return !"UNDEFINED".equals(lastCmdRecord.getJobFinalStatus());
            } else {
                return true;
            }
        } else {
            if (cmdRecords.size() == (1 + scheduling.getSubScriptIds().split(",").length)) {
                if (isYarnBatch(script)) {
                    return !"UNDEFINED".equals(lastCmdRecord.getJobFinalStatus());
                } else {
                    return true;
                }
            } else {
                if (isYarnBatch(script)) {
                    return !"UNDEFINED".equals(lastCmdRecord.getJobFinalStatus()) && !"SUCCEEDED".equals(lastCmdRecord.getJobFinalStatus());
                } else {
                    return Constant.EXEC_STATUS_FINISH != lastCmdRecord.getStatus();
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
