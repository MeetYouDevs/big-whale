package com.meiyou.bigwhale.service;

import com.meiyou.bigwhale.common.Constant;
import com.meiyou.bigwhale.dto.DtoSchedule;
import com.meiyou.bigwhale.entity.Script;
import com.meiyou.bigwhale.entity.Schedule;
import com.meiyou.bigwhale.data.service.AbstractMysqlPagingAndSortingQueryService;
import com.meiyou.bigwhale.entity.ScheduleSnapshot;
import com.meiyou.bigwhale.entity.ScriptHistory;
import com.meiyou.bigwhale.job.ScheduleJob;
import com.meiyou.bigwhale.util.SchedulerUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class ScheduleServiceImpl extends AbstractMysqlPagingAndSortingQueryService<Schedule, Integer> implements ScheduleService {

    @Autowired
    private ScheduleSnapshotService scheduleSnapshotService;
    @Autowired
    private ScriptService scriptService;
    @Autowired
    private ScriptHistoryService scriptHistoryService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Schedule entity) {
        scheduleSnapshotService.deleteByQuery("scheduleId=" + entity.getId());
        List<Script> scripts = scriptService.findByQuery("scheduleId=" + entity.getId());
        scripts.forEach(scriptService::delete);
        super.delete(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(Schedule entity, List<Script> scriptEntities) {
        if (entity.getId() != null) {
            SchedulerUtils.pauseJob(entity.getId(), Constant.JobGroup.SCHEDULE);
        }
        boolean snapshotNew = false;
        boolean cronUpdate = false;
        if (entity.getId() != null) {
            Schedule dbSchedule = findById(entity.getId());
            if (!entity.generateCron().equals(dbSchedule.generateCron())) {
                snapshotNew = true;
                cronUpdate = true;
            }
            if (!entity.getStartTime().equals(dbSchedule.getStartTime()) || !entity.getEndTime().equals(dbSchedule.getEndTime())) {
                cronUpdate = true;
            }
            if (!entity.getEnabled().equals(dbSchedule.getEnabled())) {
                cronUpdate = true;
            }
            if (!entity.generateCompareTopology().equals(dbSchedule.generateCompareTopology())) {
                snapshotNew = true;
            }
        } else  {
            snapshotNew = true;
            cronUpdate = true;
        }
        Date now = new Date();
        if (entity.getEnabled()) {
            Date needFireTime = ScheduleJob.getNeedFireTime(entity.generateCron(), entity.getStartTime().compareTo(now) <= 0 ? now : entity.getStartTime());
            Date nextFireTime = ScheduleJob.getNextFireTime(entity.generateCron(), entity.getStartTime().compareTo(now) <= 0 ? now : entity.getStartTime());
            entity.setNeedFireTime(needFireTime);
            entity.setNextFireTime(nextFireTime);
        } else {
            entity.setNeedFireTime(null);
            entity.setNextFireTime(null);
        }
        Schedule schedule = super.save(entity);

        ScheduleSnapshot scheduleSnapshot;
        if (snapshotNew) {
            scheduleSnapshot = new ScheduleSnapshot();
            BeanUtils.copyProperties(schedule, scheduleSnapshot);
            scheduleSnapshot.setId(null);
            scheduleSnapshot.setScheduleId(schedule.getId());
            scheduleSnapshot.setSnapshotTime(new Date());
            scheduleSnapshotService.save(scheduleSnapshot);
        } else {
            // 更新旧快照
            scheduleSnapshot = scheduleSnapshotService.findByScheduleIdAndSnapshotTime(schedule.getId(), now);
            Integer oldId = scheduleSnapshot.getId();
            BeanUtils.copyProperties(schedule, scheduleSnapshot);
            scheduleSnapshot.setId(oldId);
            scheduleSnapshot = scheduleSnapshotService.save(scheduleSnapshot);
        }

        for (Script scriptEntity : scriptEntities) {
            if (scriptEntity.getScheduleId() == null) {
                scriptEntity.setScheduleId(schedule.getId());
            }
            scriptService.save(scriptEntity);
        }

        if (cronUpdate || snapshotNew) {
            if (schedule.getEnabled()) {
                DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
                String scheduleInstanceId = dateFormat.format(schedule.getNextFireTime());
                List<ScriptHistory> scriptHistories = scriptHistoryService.findByQuery("scheduleId=" + scheduleSnapshot.getScheduleId() +
                        ";state=" + Constant.JobState.UN_CONFIRMED_);
                dealHistory(null, scheduleInstanceId, scheduleSnapshot, 0, scriptHistories);
                scriptHistories.forEach(scriptHistoryService::missingScheduling);
            } else {
                List<ScriptHistory> scriptHistories = scriptHistoryService.findByQuery("scheduleId=" + scheduleSnapshot.getScheduleId() + ";state=" + Constant.JobState.UN_CONFIRMED_);
                scriptHistories.forEach(scriptHistoryService::missingScheduling);
            }
            SchedulerUtils.deleteJob(schedule.getId(), Constant.JobGroup.SCHEDULE);
            if (schedule.getEnabled()) {
                ScheduleJob.build(schedule);
            }
        } else {
            if (entity.getId() != null) {
                SchedulerUtils.resumeJob(entity.getId(), Constant.JobGroup.SCHEDULE);
            }
        }

    }

    @Override
    public Page<String> instancePage(DtoSchedule req) {
        int pageNo = req.pageNo - 1;
        int pageSize = req.pageSize;
        long total;
        List<String> content;
        if (StringUtils.isBlank(req.getInstance())) {
            String countStatement = "SELECT COUNT(1) FROM (SELECT COUNT(1) FROM script_history" +
                    " WHERE schedule_id = ?" +
                    " GROUP BY schedule_instance_id) gt;";
            List<Long> totals = jdbcTemplate.queryForList(countStatement, new Object[]{req.getId()}, Long.class);
            total = totals.isEmpty() ? 0 : totals.get(0);
            String listStatement = "SELECT schedule_instance_id FROM script_history" +
                    " WHERE schedule_id = ?" +
                    " GROUP BY schedule_instance_id" +
                    " ORDER BY schedule_instance_id DESC " +
                    " LIMIT ?,?";
            content = jdbcTemplate.queryForList(listStatement, new Object[]{req.getId(), pageNo * pageSize, pageSize}, String.class);
        } else {
            String countStatement = "SELECT COUNT(1) FROM (SELECT COUNT(1) FROM script_history" +
                    " WHERE schedule_id = ?" +
                    " GROUP BY schedule_instance_id" +
                    " HAVING schedule_instance_id LIKE ?) gt";
            List<Long> totals = jdbcTemplate.queryForList(countStatement, new Object[]{req.getId(),  req.getInstance() + "%"}, Long.class);
            total = totals.isEmpty() ? 0 : totals.get(0);
            String listStatement = "SELECT schedule_instance_id FROM script_history" +
                    " WHERE schedule_id = ?" +
                    " GROUP BY schedule_instance_id" +
                    " HAVING schedule_instance_id LIKE ?" +
                    " ORDER BY schedule_instance_id DESC" +
                    " LIMIT ?,?";
            content = jdbcTemplate.queryForList(listStatement, new Object[]{req.getId(), req.getInstance() + "%", pageNo * pageSize, pageSize}, String.class);
        }
        return new PageImpl<>(content, new PageRequest(pageNo, pageSize), total);
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
