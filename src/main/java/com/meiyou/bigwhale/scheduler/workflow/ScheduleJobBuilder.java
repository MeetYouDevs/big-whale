package com.meiyou.bigwhale.scheduler.workflow;

import com.meiyou.bigwhale.common.Constant;
import com.meiyou.bigwhale.entity.ScriptHistory;
import com.meiyou.bigwhale.entity.Schedule;
import com.meiyou.bigwhale.service.ScriptHistoryService;
import com.meiyou.bigwhale.service.ScriptService;
import com.meiyou.bigwhale.service.ScheduleService;
import com.meiyou.bigwhale.util.SchedulerUtils;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * @author Suxy
 * @date 2019/9/5
 * @description file description
 */
public class ScheduleJobBuilder implements Job {

    private DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    @Autowired
    private ScriptHistoryService scriptHistoryService;
    @Autowired
    private ScriptService scriptService;
    @Autowired
    private ScheduleService scheduleService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        Integer scheduleId = Integer.parseInt(jobExecutionContext.getJobDetail().getKey().getName());
        Schedule schedule = scheduleService.findById(scheduleId);
        schedule.setRealFireTime(jobExecutionContext.getFireTime());
        schedule.setNeedFireTime(jobExecutionContext.getScheduledFireTime());
        schedule.setNextFireTime(jobExecutionContext.getNextFireTime());
        scheduleService.save(schedule);
        prepareNext(jobExecutionContext, schedule);
    }

    private void prepareNext(JobExecutionContext jobExecutionContext, Schedule schedule) {
        String scheduleInstanceId = dateFormat.format(jobExecutionContext.getNextFireTime());
        ScriptHistory scriptHistory = scriptHistoryService.findOneByQuery("scheduleId=" + schedule.getId() +
                ";scheduleInstanceId=" + scheduleInstanceId);
        if (scriptHistory != null) {
            return;
        }
        scriptService.generateHistory(schedule, scheduleInstanceId, null);
    }

    public static void build(Schedule schedule) {
        SchedulerUtils.scheduleCronJob(ScheduleJobBuilder.class,
                schedule.getId(),
                Constant.JobGroup.SCHEDULE,
                schedule.generateCron(),
                null,
                schedule.getStartTime(),
                schedule.getEndTime());
    }

}
