package com.meiyou.bigwhale.listener;

import com.meiyou.bigwhale.common.Constant;
import com.meiyou.bigwhale.entity.*;
import com.meiyou.bigwhale.scheduler.*;
import com.meiyou.bigwhale.scheduler.monitor.StreamJobMonitor;
import com.meiyou.bigwhale.scheduler.system.PlatformTimeoutChecker;
import com.meiyou.bigwhale.scheduler.system.ScriptHistoryCleaner;
import com.meiyou.bigwhale.scheduler.workflow.ScheduleJobBuilder;
import com.meiyou.bigwhale.scheduler.workflow.ScheduleJobSubmitter;
import com.meiyou.bigwhale.scheduler.workflow.ScheduleJobPreparer;
import com.meiyou.bigwhale.service.*;
import com.meiyou.bigwhale.scheduler.system.ActiveYarnAppRefresher;
import com.meiyou.bigwhale.util.SchedulerUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author Suxy
 * @date 2019/8/23
 * @description file description
 */
@Component
@ConditionalOnProperty(name = "spring.profiles.active", havingValue = "pro")
public class ApplicationReadyListener implements ApplicationListener<ApplicationReadyEvent> {

    private final Logger logger = LoggerFactory.getLogger(ApplicationReadyListener.class);
    private final DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    @Autowired
    private MonitorService monitorService;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private ScriptHistoryService scriptHistoryService;
    @Autowired
    private ScriptService scriptService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        logger.warn("Starting necessary task");
        // 启动常驻任务
        startSystem();
        // 启动监控
        startMonitor();
        // 启动任务调度
        startSchedule();

        // ------------ 作业追踪 ------------

        // 执行超时处理
        SchedulerUtils.scheduleCronJob(ScriptJobTimeoutChecker.class, "*/10 * * * * ?");
        // Yarn状态更新
        SchedulerUtils.scheduleCronJob(ScriptJobYarnStateRefresher.class, "*/10 * * * * ?");
        // 服务异常退出处理
        SchedulerUtils.scheduleSimpleJob(ScriptJobExceptionFeedbacker.class, ScriptJobExceptionFeedbacker.class.getSimpleName(), Constant.JobGroup.COMMON, 0, 0, null, DateUtils.addSeconds(new Date(), 60), null);
    }

    private void startSystem() {
        // yarn活跃应用列表更新（包含应用重复检测、长时间未运行和内存超限检测）
        SchedulerUtils.scheduleCronJob(ActiveYarnAppRefresher.class, "*/10 * * * * ?");
        // 平台执行超时处理
        SchedulerUtils.scheduleCronJob(PlatformTimeoutChecker.class, "*/10 * * * * ?");
        // 执行记录清理
        SchedulerUtils.scheduleCronJob(ScriptHistoryCleaner.class, "0 0 0 */1 * ?");
    }

    private void startMonitor() {
        List<Monitor> monitors = monitorService.findByQuery("enabled=" + true);
        monitors.forEach(StreamJobMonitor::build);
    }

    private void startSchedule() {
        List<Schedule> schedules = scheduleService.findByQuery("enabled=" + true);
        Date now = new Date();
        schedules.forEach(schedule -> {
            Date nextFireTime = SchedulerUtils.getNextFireTime(schedule.generateCron(), schedule.getStartTime().compareTo(now) <= 0 ? now : schedule.getStartTime());
            // 生成下个实例
            if (nextFireTime.compareTo(schedule.getEndTime()) <= 0) {
                String scheduleInstanceId = dateFormat.format(nextFireTime);
                ScriptHistory scriptHistory = scriptHistoryService.findOneByQuery("scheduleId=" + schedule.getId() +
                        ";scheduleInstanceId=" + scheduleInstanceId);
                if (scriptHistory == null) {
                    scriptService.reGenerateHistory(schedule, scheduleInstanceId, null);
                }
                ScheduleJobBuilder.build(schedule);
            }
        });
        SchedulerUtils.scheduleCronJob(ScheduleJobPreparer.class, ScheduleJobPreparer.class.getSimpleName(), Constant.JobGroup.SCHEDULE, "*/5 * * * * ?");
        SchedulerUtils.scheduleCronJob(ScheduleJobSubmitter.class, ScheduleJobSubmitter.class.getSimpleName(), Constant.JobGroup.SCHEDULE, "*/1 * * * * ?");
    }

}

