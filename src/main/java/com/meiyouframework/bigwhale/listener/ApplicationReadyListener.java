package com.meiyouframework.bigwhale.listener;

import com.meiyouframework.bigwhale.common.Constant;
import com.meiyouframework.bigwhale.entity.Scheduling;
import com.meiyouframework.bigwhale.service.SchedulingService;
import com.meiyouframework.bigwhale.task.common.*;
import com.meiyouframework.bigwhale.task.streaming.AbstractMonitorRunner;
import com.meiyouframework.bigwhale.task.batch.DagTaskAppStatusUpdateJob;
import com.meiyouframework.bigwhale.task.batch.DagTask;
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
    private SchedulingService schedulingService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        logger.warn("Starting necessary task");
        //启动常驻任务
        startResidentMission();
        //启动任务调度
        startScheduling();
    }

    private void startResidentMission() {
        try {
            //启动yarn活跃应用列表更新任务（包含应用重复检测和大内存应用检测）
            SchedulerUtils.scheduleCornJob(RefreshActiveStateAppsJob.class, "*/10 * * * * ?");
            //启动脚本执行超时处理任务
            SchedulerUtils.scheduleCornJob(CmdRecordTimeoutJob.class, "*/1 * * * * ?");
            //启动执行记录清理任务
            SchedulerUtils.scheduleCornJob(CmdRecordClearJob.class, "0 0 0 */1 * ?");
            //平台执行超时处理任务
            SchedulerUtils.scheduleCornJob(PlatformTimeoutJob.class, "*/10 * * * * ?");
        } catch (SchedulerException e) {
            logger.error("schedule submit error", e);
        }
    }

    private void startScheduling() {
        try {
            //启动批处理应用状态更新任务（包含提交子脚本）
            SchedulerUtils.scheduleCornJob(DagTaskAppStatusUpdateJob.class, DagTaskAppStatusUpdateJob.class.getSimpleName(),  Constant.JobGroup.BATCH, "*/10 * * * * ?");
            List<Scheduling> schedulings = schedulingService.findByQuery("enabled=" + true);
            for (Scheduling scheduling : schedulings) {
                if (new Date().after(scheduling.getEndTime())) {
                    if (scheduling.getType() == Constant.SCHEDULING_TYPE_BATCH) {
                        SchedulerUtils.deleteJob(scheduling.getId(), Constant.JobGroup.BATCH);
                    } else {
                        SchedulerUtils.deleteJob(scheduling.getId(), Constant.JobGroup.STREAMING);
                    }
                    scheduling.setEnabled(false);
                    schedulingService.save(scheduling);
                } else {
                    if (scheduling.getType() == Constant.SCHEDULING_TYPE_BATCH) {
                        DagTask.build(scheduling);
                    } else {
                        AbstractMonitorRunner.build(scheduling);
                    }
                }
            }
        } catch (SchedulerException e) {
            logger.error("schedule submit error", e);
        }
    }

}

