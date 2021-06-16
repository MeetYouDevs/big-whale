package com.meiyou.bigwhale.service;

import com.meiyou.bigwhale.data.service.PagingAndSortingQueryService;
import com.meiyou.bigwhale.dto.DtoScript;
import com.meiyou.bigwhale.entity.Monitor;
import com.meiyou.bigwhale.entity.Schedule;
import com.meiyou.bigwhale.entity.Script;
import com.meiyou.bigwhale.entity.ScriptHistory;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ScriptService extends PagingAndSortingQueryService<Script, Integer> {

    Page<Script> fuzzyPage(DtoScript req);

    String validate(DtoScript req);

    void update(Script entity, Monitor monitorEntity);

    ScriptHistory generateHistory(Script script);

    ScriptHistory generateHistory(Script script, Monitor monitor);

    /**
     *
     * @param script
     * @param schedule
     * @param scheduleInstanceId
     * @param generateStatus
     * @return 0 需确认 1 确认 2补数
     */
    ScriptHistory generateHistory(Script script, Schedule schedule, String scheduleInstanceId, String previousScheduleTopNodeId, int generateStatus);

    void reGenerateHistory(Schedule schedule, String scheduleInstanceId, String previousScheduleTopNodeId, int generateStatus, List<ScriptHistory> scriptHistories);

    String extractJarPath(String content);

    void deleteJar(Script entity);
}
