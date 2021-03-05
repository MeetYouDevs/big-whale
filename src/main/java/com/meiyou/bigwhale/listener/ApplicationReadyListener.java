package com.meiyou.bigwhale.listener;

import com.meiyou.bigwhale.common.Constant;
import com.meiyou.bigwhale.entity.*;
import com.meiyou.bigwhale.job.*;
import com.meiyou.bigwhale.job.system.PlatformTimeoutJob;
import com.meiyou.bigwhale.service.*;
import com.meiyou.bigwhale.job.system.ActiveYarnAppRefreshJob;
import com.meiyou.bigwhale.util.SchedulerUtils;
import org.quartz.SchedulerException;
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
import java.util.Map;

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
    private ScheduleService scheduleService;
    @Autowired
    private MonitorService monitorService;
    @Autowired
    private ScriptHistoryService scriptHistoryService;
    @Autowired
    private ScriptService scriptService;
    @Autowired
    private ScheduleSnapshotService scheduleSnapshotService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        logger.warn("Starting necessary task");
        // 启动常驻任务
        startResidentMission();
        // 启动任务调度
        startSchedule();
        // 启动监控
        startMonitor();
        try {
            SchedulerUtils.getScheduler().start();
        } catch (SchedulerException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void startResidentMission() {
        // 启动yarn活跃应用列表更新任务（包含应用重复检测、长时间未运行和内存超限检测）
        SchedulerUtils.scheduleCronJob(ActiveYarnAppRefreshJob.class, "*/10 * * * * ?");
        // 启动作业状态更新任务
        SchedulerUtils.scheduleCronJob(ScriptHistoryYarnStateRefreshJob.class, "*/5 * * * * ?");
        // 启动调度记录提交任务
        SchedulerUtils.scheduleCronJob(ScheduleSubmitJob.class, "*/1 * * * * ?");
        // 启动脚本执行超时处理任务
        SchedulerUtils.scheduleCronJob(ScriptHistoryTimeoutJob.class, "*/10 * * * * ?");
        // 启动执行记录清理任务
        SchedulerUtils.scheduleCronJob(ScriptHistoryClearJob.class, "0 0 0 */1 * ?");
        // 平台执行超时处理任务
        SchedulerUtils.scheduleCronJob(PlatformTimeoutJob.class, "*/10 * * * * ?");
    }

    private void startSchedule() {
        List<Schedule> schedules = scheduleService.findByQuery("enabled=" + true);
        Date now = new Date();
        schedules.forEach(schedule -> {
            Date nextFireTime = ScheduleJob.getNextFireTime(schedule.generateCron(), schedule.getStartTime().compareTo(now) <= 0 ? now : schedule.getStartTime());
            String scheduleInstanceId = dateFormat.format(nextFireTime);
            List<ScriptHistory> scriptHistories = scriptHistoryService.findByQuery("scheduleId=" + schedule.getId() + ";state=" + Constant.JobState.UN_CONFIRMED_);
            ScheduleSnapshot scheduleSnapshot = scheduleSnapshotService.findByScheduleIdAndSnapshotTime(schedule.getId(), now);
            dealHistory(null, scheduleInstanceId, scheduleSnapshot, 0, scriptHistories);
            ScheduleJob.build(schedule);
        });
    }

    private void startMonitor() {
        List<Monitor> monitors = monitorService.findByQuery("enabled=" + true);
        monitors.forEach(MonitorJob::build);
    }

    private void dealHistory(String scheduleTopNodeId, String scheduleInstanceId, ScheduleSnapshot scheduleSnapshot, int generateStatus, List<ScriptHistory> scriptHistories) {
        Map<String, ScheduleSnapshot.Topology.Node> nextNodeIdToObj = scheduleSnapshot.analyzeNextNode(scheduleTopNodeId);
        for (String nodeId : nextNodeIdToObj.keySet()) {
            boolean exist = scriptHistories.removeIf(scriptHistory ->
                    scriptHistory.getScheduleTopNodeId().equals(nodeId) && scriptHistory.getScheduleInstanceId().equals(scheduleInstanceId));
            if (!exist) {
                Script script = scriptService.findOneByQuery("scheduleId=" + scheduleSnapshot.getScheduleId() +  ";scheduleTopNodeId=" + nodeId);
                scriptService.generateHistory(script, scheduleSnapshot, scheduleInstanceId, generateStatus);
            }
            dealHistory(nodeId, scheduleInstanceId, scheduleSnapshot, generateStatus, scriptHistories);
        }
    }

}

