package com.meiyou.bigwhale.service;

import com.meiyou.bigwhale.dto.DtoSchedule;
import com.meiyou.bigwhale.entity.Script;
import com.meiyou.bigwhale.entity.Schedule;
import com.meiyou.bigwhale.data.service.AbstractMysqlPagingAndSortingQueryService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ScheduleServiceImpl extends AbstractMysqlPagingAndSortingQueryService<Schedule, Integer> implements ScheduleService {

    @Autowired
    private ScriptService scriptService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Schedule entity) {
        List<Script> scripts = scriptService.findByQuery("scheduleId=" + entity.getId());
        scripts.forEach(scriptService::delete);
        super.delete(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Schedule update(Schedule entity, List<Script> scriptEntities) {
        Schedule schedule = super.save(entity);
        for (Script scriptEntity : scriptEntities) {
            if (scriptEntity.getScheduleId() == null) {
                scriptEntity.setScheduleId(schedule.getId());
            }
            scriptService.save(scriptEntity);
        }
        return schedule;
    }

    @Override
    public Page<String> instancePage(DtoSchedule req) {
        int pageNo = req.pageNo - 1;
        int pageSize = req.pageSize;
        long total;
        List<String> content;
        if (StringUtils.isBlank(req.getInstance())) {
            String countStatement = "SELECT " +
                    "   COUNT(1) " +
                    "FROM " +
                    "   script_history " +
                    "WHERE " +
                    "   schedule_id = ? AND " +
                    "   schedule_retry = false AND " +
                    "   schedule_empty = false AND " +
                    "   schedule_rerun = false AND " +
                    "   previous_schedule_top_node_id IS NULL;";
            List<Long> totals = jdbcTemplate.queryForList(countStatement, new Object[]{req.getId()}, Long.class);
            total = totals.isEmpty() ? 0 : totals.get(0);
            String listStatement = "SELECT " +
                    "   schedule_instance_id " +
                    "FROM " +
                    "   script_history " +
                    "WHERE " +
                    "   schedule_id = ? AND " +
                    "   schedule_retry = false AND " +
                    "   schedule_empty = false AND " +
                    "   schedule_rerun = false AND " +
                    "   previous_schedule_top_node_id IS NULL " +
                    "ORDER BY " +
                    "   schedule_instance_id DESC " +
                    "LIMIT ?, ?;";
            content = jdbcTemplate.queryForList(listStatement, new Object[]{req.getId(), pageNo * pageSize, pageSize}, String.class);
        } else {
            String countStatement = "SELECT " +
                    "   COUNT(1) " +
                    "FROM " +
                    "   script_history " +
                    "WHERE " +
                    "   schedule_id = ? AND " +
                    "   schedule_retry = false AND " +
                    "   schedule_empty = false AND " +
                    "   schedule_rerun = false AND " +
                    "   previous_schedule_top_node_id IS NULL AND " +
                    "   schedule_instance_id LIKE ?;";
            List<Long> totals = jdbcTemplate.queryForList(countStatement, new Object[]{req.getId(),  req.getInstance() + "%"}, Long.class);
            total = totals.isEmpty() ? 0 : totals.get(0);
            String listStatement = "SELECT " +
                    "   schedule_instance_id " +
                    "FROM " +
                    "   script_history " +
                    "WHERE " +
                    "   schedule_id = ? AND " +
                    "   schedule_retry = false AND " +
                    "   schedule_empty = false AND " +
                    "   schedule_rerun = false AND " +
                    "   previous_schedule_top_node_id IS NULL AND " +
                    "   schedule_instance_id LIKE ? " +
                    "ORDER BY " +
                    "   schedule_instance_id DESC " +
                    "LIMIT ?, ?;";
            content = jdbcTemplate.queryForList(listStatement, new Object[]{req.getId(), req.getInstance() + "%", pageNo * pageSize, pageSize}, String.class);
        }
        return new PageImpl<>(content, new PageRequest(pageNo, pageSize), total);
    }

}
