package com.meiyouframework.bigwhale.util;

import com.meiyouframework.bigwhale.common.Constant;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Date;

/**
 * @author Suxy
 * @date 2019/8/30
 * @description file description
 */
public class SchedulerUtils {

    private static Scheduler scheduler;

    static {
        if (SpringContextUtils.getApplicationContext() != null) {
            scheduler = SpringContextUtils.getBean(Scheduler.class);
        } else {
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            try {
                scheduler = schedulerFactory.getScheduler();
                scheduler.start();
            } catch (SchedulerException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private SchedulerUtils() {

    }

    public static Scheduler getScheduler() {
        return scheduler;
    }

    public static void scheduleCornJob(Class<? extends Job> jobClass, String cronExpression) throws SchedulerException {
        scheduleCornJob(jobClass, jobClass.getSimpleName(), cronExpression);
    }

    public static void scheduleCornJob(Class<? extends Job> jobClass, String name, String cronExpression) throws SchedulerException {
        scheduleCornJob(jobClass, name, Constant.JobGroup.COMMON, cronExpression);
    }

    public static void scheduleCornJob(Class<? extends Job> jobClass, String name, String group, String cronExpression) throws SchedulerException {
        scheduleCornJob(jobClass, name, group, cronExpression, null);
    }

    public static void scheduleCornJob(Class<? extends Job> jobClass, String name, String group, String cronExpression, JobDataMap jobDataMap) throws SchedulerException {
        JobKey jobKey = new JobKey(name, group);
        if (!scheduler.checkExists(jobKey)) {
            JobBuilder jobBuilder = JobBuilder.newJob(jobClass);
            jobBuilder.withIdentity(jobKey);
            if (jobDataMap != null && !jobDataMap.isEmpty()) {
                jobBuilder.setJobData(jobDataMap);
            }
            JobDetail jobDetail = jobBuilder.build();
            CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(cronExpression).withMisfireHandlingInstructionDoNothing();
            TriggerBuilder<CronTrigger> triggerBuilder = TriggerBuilder.newTrigger().withSchedule(cronScheduleBuilder);
            CronTrigger trigger = triggerBuilder.build();
            scheduler.scheduleJob(jobDetail, trigger);
        }
    }

    public static void scheduleCornJob(Class<? extends Job> jobClass, String name, String group, String cronExpression, JobDataMap jobDataMap, Date startDate, Date endDate) throws SchedulerException {
        JobKey jobKey = new JobKey(name, group);
        if (!scheduler.checkExists(jobKey)) {
            JobBuilder jobBuilder = JobBuilder.newJob(jobClass);
            jobBuilder.withIdentity(jobKey);
            if (jobDataMap != null && !jobDataMap.isEmpty()) {
                jobBuilder.setJobData(jobDataMap);
            }
            JobDetail jobDetail = jobBuilder.build();
            CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(cronExpression).withMisfireHandlingInstructionDoNothing();
            TriggerBuilder<CronTrigger> triggerBuilder = TriggerBuilder.newTrigger().withSchedule(cronScheduleBuilder);
            if (startDate != null) {
                triggerBuilder.startAt(startDate);
            } else {
                triggerBuilder.startNow();
            }
            if (endDate != null) {
                triggerBuilder.endAt(endDate);
            }
            CronTrigger trigger = triggerBuilder.build();
            scheduler.scheduleJob(jobDetail, trigger);
        }
    }

    /**
     * 默认立即执行且只执行一次
     * @param jobClass
     * @throws SchedulerException
     */
    public static void scheduleSimpleJob(Class<? extends Job> jobClass) throws SchedulerException {
        scheduleSimpleJob(jobClass, jobClass.getSimpleName());
    }

    public static void scheduleSimpleJob(Class<? extends Job> jobClass, String name) throws SchedulerException {
        scheduleSimpleJob(jobClass, name, 0, 0);
    }

    /**
     * @param jobClass
     * @param name
     * @param intervalInMilliseconds 执行间隔
     * @param repeatCount 重复次数，小于0的时候重复执行
     * @throws SchedulerException
     */
    public static void scheduleSimpleJob(Class<? extends Job> jobClass, String name, long intervalInMilliseconds, int repeatCount) throws SchedulerException {
        scheduleSimpleJob(jobClass, name, Constant.JobGroup.COMMON, intervalInMilliseconds, repeatCount);
    }

    public static void scheduleSimpleJob(Class<? extends Job> jobClass, String name, String group, long intervalInMilliseconds, int repeatCount) throws SchedulerException {
        scheduleSimpleJob(jobClass, name, group, intervalInMilliseconds, repeatCount, null);
    }

    public static void scheduleSimpleJob(Class<? extends Job> jobClass, String name, String group, long intervalInMilliseconds, int repeatCount, JobDataMap jobDataMap) throws SchedulerException {
        scheduleSimpleJob(jobClass, name, group, intervalInMilliseconds, repeatCount, jobDataMap, null, null);
    }

    public static void scheduleSimpleJob(Class<? extends Job> jobClass, String name, String group, long intervalInMilliseconds, int repeatCount, JobDataMap jobDataMap, Date startDate, Date endDate) throws SchedulerException {
        JobKey jobKey = new JobKey(name, group);
        if (!scheduler.checkExists(jobKey)) {
            JobBuilder jobBuilder = JobBuilder.newJob(jobClass);
            jobBuilder.withIdentity(jobKey);
            if (jobDataMap != null && !jobDataMap.isEmpty()) {
                jobBuilder.setJobData(jobDataMap);
            }
            JobDetail jobDetail = jobBuilder.build();
            SimpleScheduleBuilder simpleScheduleBuilder = SimpleScheduleBuilder.simpleSchedule();
            simpleScheduleBuilder.withIntervalInMilliseconds(intervalInMilliseconds);
            if (repeatCount >= 0) {
                simpleScheduleBuilder.withRepeatCount(repeatCount);
            } else {
                simpleScheduleBuilder.repeatForever();
            }
            TriggerBuilder<SimpleTrigger> triggerBuilder = TriggerBuilder.newTrigger().withSchedule(simpleScheduleBuilder);
            if (startDate != null) {
                triggerBuilder.startAt(startDate);
            } else {
                triggerBuilder.startNow();
            }
            if (endDate != null) {
                triggerBuilder.endAt(endDate);
            }
            SimpleTrigger trigger = triggerBuilder.build();
            scheduler.scheduleJob(jobDetail, trigger);
        }
    }

    public static void deleteJob(String name, String group) throws SchedulerException {
        JobKey jobKey = new JobKey(name, group);
        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
        }
    }

    public static boolean checkExists(String name, String group) throws SchedulerException {
        JobKey jobKey = new JobKey(name, group);
        return scheduler.checkExists(jobKey);
    }

}
