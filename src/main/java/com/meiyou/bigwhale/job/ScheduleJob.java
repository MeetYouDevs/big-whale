package com.meiyou.bigwhale.job;

import com.meiyou.bigwhale.common.Constant;
import com.meiyou.bigwhale.entity.Script;
import com.meiyou.bigwhale.entity.ScriptHistory;
import com.meiyou.bigwhale.entity.Schedule;
import com.meiyou.bigwhale.entity.ScheduleSnapshot;
import com.meiyou.bigwhale.service.ScriptHistoryService;
import com.meiyou.bigwhale.service.ScriptService;
import com.meiyou.bigwhale.service.ScheduleService;
import com.meiyou.bigwhale.service.ScheduleSnapshotService;
import com.meiyou.bigwhale.util.SchedulerUtils;
import org.apache.commons.lang.time.DateUtils;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.DateFormat;
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
    @Autowired
    private ScheduleSnapshotService scheduleSnapshotService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        Integer scheduleId = Integer.parseInt(jobExecutionContext.getJobDetail().getKey().getName());
        Schedule schedule = scheduleService.findById(scheduleId);
        schedule.setRealFireTime(jobExecutionContext.getFireTime());
        schedule.setNeedFireTime(jobExecutionContext.getScheduledFireTime());
        schedule.setNextFireTime(jobExecutionContext.getNextFireTime());
        scheduleService.save(schedule);
        ScheduleSnapshot scheduleSnapshot = scheduleSnapshotService.findByScheduleIdAndSnapshotTime(scheduleId, jobExecutionContext.getScheduledFireTime());
        if (scheduleSnapshot == null) {
            return;
        }
        confirmedNeed(jobExecutionContext, scheduleSnapshot);
    }

    private void confirmedNeed(JobExecutionContext jobExecutionContext, ScheduleSnapshot scheduleSnapshot) {
        String scheduleInstanceId = dateFormat.format(jobExecutionContext.getScheduledFireTime());
        List<ScriptHistory> scriptHistories = scriptHistoryService.findByQuery("scheduleId=" + scheduleSnapshot.getScheduleId() +
                ";scheduleInstanceId=" + scheduleInstanceId + ";state=" + Constant.JobState.UN_CONFIRMED_);
        if (scriptHistories.isEmpty()) {
            scheduleInstanceId = dateFormat.format(jobExecutionContext.getNextFireTime());
            scriptHistories = scriptHistoryService.findByQuery("scheduleId=" + scheduleSnapshot.getScheduleId() +
                    ";scheduleInstanceId=" + scheduleInstanceId);
            if (scriptHistories.isEmpty()) {
                scriptHistories = scriptHistoryService.findByQuery("scheduleId=" + scheduleSnapshot.getScheduleId() +
                        ";state=" + Constant.JobState.UN_CONFIRMED_);
                scriptHistories.forEach(scriptHistoryService::missingScheduling);
                prepareNext(jobExecutionContext, scheduleSnapshot);
            }
            return;
        }
        generateHistory(null, scheduleInstanceId, scheduleSnapshot, 1);
        prepareNext(jobExecutionContext, scheduleSnapshot);
    }

    private void prepareNext(JobExecutionContext jobExecutionContext, ScheduleSnapshot scheduleSnapshot) {
        String scheduleInstanceId = dateFormat.format(jobExecutionContext.getNextFireTime());
        generateHistory(null, scheduleInstanceId, scheduleSnapshot, 0);
    }

    private void generateHistory(String scheduleTopNodeId, String scheduleInstanceId, ScheduleSnapshot scheduleSnapshot, int generateStatus) {
        Map<String, ScheduleSnapshot.Topology.Node> nextNodeIdToObj = scheduleSnapshot.analyzeNextNode(scheduleTopNodeId);
        for (String nodeId : nextNodeIdToObj.keySet()) {
            Script script = scriptService.findOneByQuery("scheduleId=" + scheduleSnapshot.getScheduleId() +  ";scheduleTopNodeId=" + nodeId);
            scriptService.generateHistory(script, scheduleSnapshot, scheduleInstanceId, generateStatus);
            generateHistory(nodeId, scheduleInstanceId, scheduleSnapshot, generateStatus);
        }
    }

    public static Date getPreviousFireTime(String cron, Date startDate) {
        Date nextFireTime1 = getNextFireTime(cron, startDate);
        Date nextFireTime2 = getNextFireTime(cron, nextFireTime1);
        int intervals = (int) (nextFireTime2.getTime() - nextFireTime1.getTime());
        Date cal1 = DateUtils.addMilliseconds(nextFireTime1, - intervals);
        Date cal2 = getNextFireTime(cron, cal1);
        Date cal3 = getNextFireTime(cron, cal2);
        Date cal4 = getNextFireTime(cron, cal3);
        while (cal4.compareTo(nextFireTime1) > 0) {
            cal1 = DateUtils.addMilliseconds(cal1, - intervals);
            cal2 = getNextFireTime(cron, cal1);
            cal3 = getNextFireTime(cron, cal2);
            cal4 = getNextFireTime(cron, cal3);
        }
        if (cal4.equals(nextFireTime1)) {
            return cal2;
        }
        return cal3;
    }

    public static Date getNeedFireTime(String cron, Date startDate) {
        Date nextFireTime1 = getNextFireTime(cron, startDate);
        Date nextFireTime2 = getNextFireTime(cron, nextFireTime1);
        int intervals = (int) (nextFireTime2.getTime() - nextFireTime1.getTime());
        Date cal1 = DateUtils.addMilliseconds(nextFireTime1, - intervals);
        Date cal2 = getNextFireTime(cron, cal1);
        Date cal3 = getNextFireTime(cron, cal2);
        if (cal3.equals(nextFireTime1)) {
            return cal2;
        } else {
            Date cal4 = getNextFireTime(cron, cal3);
            while (cal4.compareTo(nextFireTime1) > 0) {
                cal1 = DateUtils.addMilliseconds(cal1, - intervals);
                cal2 = getNextFireTime(cron, cal1);
                cal3 = getNextFireTime(cron, cal2);
                cal4 = getNextFireTime(cron, cal3);
            }
            if (cal4.equals(nextFireTime1)) {
                return cal3;
            }
            return cal2;
        }
    }

    public static Date getNextFireTime(String cron, Date startDate) {
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(cron);
        TriggerBuilder<CronTrigger> triggerBuilder = TriggerBuilder.newTrigger().withSchedule(cronScheduleBuilder);
        triggerBuilder.startAt(startDate);
        CronTrigger trigger = triggerBuilder.build();
        return trigger.getFireTimeAfter(startDate);
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
