package com.meiyou.bigwhale.service;

import com.meiyou.bigwhale.data.service.AbstractMysqlPagingAndSortingQueryService;
import com.meiyou.bigwhale.entity.ScriptHistory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class ScriptHistoryServiceImpl extends AbstractMysqlPagingAndSortingQueryService<ScriptHistory, Integer> implements ScriptHistoryService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteFuture(Integer scheduleId, Date date) {
        jdbcTemplate.update("DELETE FROM " +
                "   script_history " +
                "WHERE " +
                "   schedule_id = ? AND " +
                "   business_time > ?;", scheduleId, date);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void switchScheduleRunnable(Integer id, boolean scheduleRunnable) {
        jdbcTemplate.update("UPDATE script_history SET schedule_runnable = ? WHERE id = ?;", scheduleRunnable, id);
    }

    @Override
    public ScriptHistory findScriptLatest(Integer scriptId) {
        return findOneByQuery("scriptId=" + scriptId, new Sort(Sort.Direction.DESC, "id"));
    }

}
