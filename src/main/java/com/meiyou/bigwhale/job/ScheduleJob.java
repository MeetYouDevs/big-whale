package com.meiyou.bigwhale.job;

import com.meiyou.bigwhale.common.Constant;
import com.meiyou.bigwhale.entity.Script;
import com.meiyou.bigwhale.entity.ScriptHistory;
import com.meiyou.bigwhale.entity.Schedule;
import com.meiyou.bigwhale.service.ScriptHistoryService;
import com.meiyou.bigwhale.service.ScriptService;
import com.meiyou.bigwhale.service.ScheduleService;
import com.meiyou.bigwhale.util.SchedulerUtils;
import org.apache.commons.lang.time.DateUtils;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Suxy
 * @date 2019/9/5
 * @description file description
 */
public class ScheduleJob implements Job {

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
        confirmedNeed(jobExecutionContext, schedule);
    }

    private void confirmedNeed(JobExecutionContext jobExecutionContext, Schedule schedule) {
        String scheduleInstanceId = dateFormat.format(jobExecutionContext.getScheduledFireTime());
        List<ScriptHistory> scriptHistories = scriptHistoryService.findByQuery("scheduleId=" + schedule.getId() +
                ";scheduleInstanceId=" + scheduleInstanceId + ";state=" + Constant.JobState.UN_CONFIRMED_);
        if (scriptHistories.isEmpty()) {
            scheduleInstanceId = dateFormat.format(jobExecutionContext.getNextFireTime());
            scriptHistories = scriptHistoryService.findByQuery("scheduleId=" + schedule.getId() +
                    ";scheduleInstanceId=" + scheduleInstanceId);
            if (scriptHistories.isEmpty()) {
                scriptHistories = scriptHistoryService.findByQuery("scheduleId=" + schedule.getId() +
                        ";state=" + Constant.JobState.UN_CONFIRMED_);
                scriptHistories.forEach(scriptHistoryService::missingScheduling);
                prepareNext(jobExecutionContext, schedule);
            }
            return;
        }
        generateHistory(schedule, scheduleInstanceId, null, 1);
        prepareNext(jobExecutionContext, schedule);
    }

    private void prepareNext(JobExecutionContext jobExecutionContext, Schedule schedule) {
        String scheduleInstanceId = dateFormat.format(jobExecutionContext.getNextFireTime());
        generateHistory(schedule, scheduleInstanceId, null, 0);
    }

    private void generateHistory(Schedule schedule, String scheduleInstanceId, String previousScheduleTopNodeId, int generateStatus) {
        Map<String, Schedule.Topology.Node> nextNodeIdToObj = schedule.analyzeNextNode(previousScheduleTopNodeId);
        for (String nodeId : nextNodeIdToObj.keySet()) {
            Script script = scriptService.findOneByQuery("scheduleId=" + schedule.getId() +  ";scheduleTopNodeId=" + nodeId);
            scriptService.generateHistory(script, schedule, scheduleInstanceId, previousScheduleTopNodeId, generateStatus);
            generateHistory(schedule, scheduleInstanceId, nodeId, generateStatus);
        }
    }

    public static Date getNeedFireTime(String cron, Date startDate) {
        Date nextFireTime1 = getNextFireTime(cron, startDate);
        Date nextFireTime2 = getNextFireTime(cron, nextFireTime1);
        int intervals = (int) (nextFireTime2.getTime() - nextFireTime1.getTime());
        Date cal1 = DateUtils.addMilliseconds(nextFireTime1, - intervals);
        Date cal2 = getNextFireTime(cron, cal1);
        Date cal3 = getNextFireTime(cron, cal2);
        while (!cal3.equals(nextFireTime1)) {
            cal1 = DateUtils.addMilliseconds(cal1, - intervals);
            cal2 = getNextFireTime(cron, cal1);
            cal3 = getNextFireTime(cron, cal2);
            if (cal3.before(nextFireTime1)) {
                intervals = -1000;
            }
        }
        return cal2;
    }

    public static Date getNextFireTime(String cron, Date startDate) {
        return getCronExpression(cron).getTimeAfter(startDate);
    }

    private static CronExpression getCronExpression(String cron) {
        try {
            return new CronExpression(cron);
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static void build(Schedule schedule) {
        SchedulerUtils.scheduleCronJob(ScheduleJob.class,
                schedule.getId(),
                Constant.JobGroup.SCHEDULE,
                schedule.generateCron(),
                null,
                schedule.getStartTime(),
                schedule.getEndTime());
    }

}
