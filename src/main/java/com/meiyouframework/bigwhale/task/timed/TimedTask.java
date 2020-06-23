package com.meiyouframework.bigwhale.task.timed;

import com.alibaba.fastjson.JSON;
import com.meiyouframework.bigwhale.common.Constant;
import com.meiyouframework.bigwhale.util.YarnApiUtils;
import com.meiyouframework.bigwhale.task.cmd.CmdRecordRunner;
import com.meiyouframework.bigwhale.util.SchedulerUtils;
import com.meiyouframework.bigwhale.util.SpringContextUtils;
import com.meiyouframework.bigwhale.entity.*;
import com.meiyouframework.bigwhale.service.*;
import org.apache.commons.lang.StringUtils;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * @author Suxy
 * @date 2019/9/5
 * @description file description
 */
@DisallowConcurrentExecution
public class TimedTask implements Job {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimedTask.class);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        String taskTimerId = jobExecutionContext.getJobDetail().getKey().getName();
        SchedulingService schedulingService = SpringContextUtils.getBean(SchedulingService.class);
        Scheduling scheduling = schedulingService.findById(taskTimerId);
        ScriptService scriptService = SpringContextUtils.getBean(ScriptService.class);
        Script script = scriptService.findById(scheduling.getScriptId());
        scheduling.setLastExecuteTime(new Date());
        schedulingService.save(scheduling);
        AgentService agentService = SpringContextUtils.getBean(AgentService.class);
        String agentId = script.getAgentId();
        if (StringUtils.isBlank(agentId)) {
            Agent agent = agentService.getByClusterId(script.getClusterId());
            if (agent != null) {
                agentId = agent.getId();
            }
        }
        // 不可重复提交
        if (scheduling.getRepeatSubmit() != null && !scheduling.getRepeatSubmit()) {
            //排除脚本任务
            if (script.getType() != Constant.SCRIPT_TYPE_SHELL) {
                YarnAppService yarnAppService = SpringContextUtils.getBean(YarnAppService.class);
                ClusterService clusterService = SpringContextUtils.getBean(ClusterService.class);
                //先从yarn_app中查找
                YarnApp yarnApp = yarnAppService.findOneByQuery("scriptId=" + script.getId());
                if (yarnApp != null) {
                    if (yarnApp.getName().equals(script.getApp())) {
                        return;
                    }
                }
                //如果不存在，再实时请求查询一次
                Cluster cluster = clusterService.findById(script.getClusterId());
                if (cluster != null) {
                    if (YarnApiUtils.getActiveApp(cluster.getYarnUrl(), script.getUser(), script.getQueue(), script.getApp(), 1) != null) {
                        return;
                    }
                }
            }
        }
        CmdRecordService cmdRecordService = SpringContextUtils.getBean(CmdRecordService.class);
        CmdRecord cmdRecord = CmdRecord.builder()
                .uid(scheduling.getUid())
                .scriptId(script.getId())
                .subScriptIds(scheduling.getSubScriptIds())
                .createTime(new Date())
                .content(script.getScript())
                .timeOut(script.getTimeOut())
                .status(Constant.EXEC_STATUS_UNSTART)
                .agentId(agentId)
                .clusterId(script.getClusterId())
                .schedulingId(scheduling.getId())
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
