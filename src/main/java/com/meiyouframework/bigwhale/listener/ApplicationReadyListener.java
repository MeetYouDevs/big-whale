package com.meiyouframework.bigwhale.listener;

import com.meiyouframework.bigwhale.common.Constant;
import com.meiyouframework.bigwhale.entity.Monitor;
import com.meiyouframework.bigwhale.entity.Scheduling;
import com.meiyouframework.bigwhale.service.MonitorService;
import com.meiyouframework.bigwhale.service.SchedulingService;
import com.meiyouframework.bigwhale.task.common.*;
import com.meiyouframework.bigwhale.task.monitor.AbstractMonitorRunner;
import com.meiyouframework.bigwhale.task.timed.TimedTask;
import com.meiyouframework.bigwhale.util.SchedulerUtils;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

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

    @Autowired
    private MonitorService monitorService;
    @Autowired
    private SchedulingService schedulingService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        logger.warn("Starting necessary task");
        //启动常驻任务
        startResidentMission();
        //启动监控任务
        startMonitor();
        //启动定时任务
        startTimerTask();
    }

    private void startResidentMission() {
        try {
            //启动yarn活跃应用列表更新任务（包含应用重复检测和大内存应用检测）
            SchedulerUtils.scheduleCornJob(RefreshActiveStateAppsJob.class, "*/10 * * * * ?");
            //启动脚本执行超时处理任务
            SchedulerUtils.scheduleCornJob(CmdRecordTimeoutJob.class, "*/1 * * * * ?");
            //启动离线应用状态更新任务（包含提交子脚本）
            SchedulerUtils.scheduleCornJob(CmdRecordAppStatusUpdateJob.class, "*/10 * * * * ?");
            //启动执行记录清理任务
            SchedulerUtils.scheduleCornJob(CmdRecordClearJob.class, "0 0 0 */1 * ?");
            //平台执行超时处理任务
            SchedulerUtils.scheduleCornJob(PlatformTimeoutJob.class, "*/10 * * * * ?");
        } catch (SchedulerException e) {
            logger.error("schedule submit error", e);
        }
    }

    private void startMonitor() {
        List<Monitor> monitors = monitorService.findByQuery("status=" + Constant.STATUS_ON);
        monitors.forEach(monitorInfo -> {
            try {
                AbstractMonitorRunner.build(monitorInfo);
            } catch (SchedulerException e) {
                logger.error("schedule submit error", e);
            }
        });
    }

    private void startTimerTask() {
        List<Scheduling> schedulings = schedulingService.findByQuery("status=" + Constant.STATUS_ON);
        schedulings.forEach(taskTimer -> {
            try {
                if (new Date().after(taskTimer.getEndTime())) {
                    SchedulerUtils.deleteJob(taskTimer.getId(), Constant.JobGroup.TIMED);
                    taskTimer.setStatus(Constant.STATUS_OFF);
                    schedulingService.save(taskTimer);
                } else {
                    TimedTask.build(taskTimer);
                }
            } catch (SchedulerException e) {
                logger.error("schedule submit error", e);
            }
        });
    }

}

