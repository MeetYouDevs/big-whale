package com.meiyou.bigwhale.service;

import com.meiyou.bigwhale.data.service.PagingAndSortingQueryService;
import com.meiyou.bigwhale.dto.DtoScript;
import com.meiyou.bigwhale.entity.Monitor;
import com.meiyou.bigwhale.entity.Schedule;
import com.meiyou.bigwhale.entity.Script;
import com.meiyou.bigwhale.entity.ScriptHistory;
import org.springframework.data.domain.Page;

public interface ScriptService extends PagingAndSortingQueryService<Script, Integer> {

    Page<Script> fuzzyPage(DtoScript req);

    String validate(DtoScript req);

    Script update(Script entity, Monitor monitorEntity);

    ScriptHistory generateHistory(Script script);

    ScriptHistory generateHistory(Script script, Monitor monitor);

    void generateHistory(Schedule schedule, String scheduleInstanceId, String previousScheduleTopNodeId);

    void reGenerateHistory(Schedule schedule, String scheduleInstanceId, String previousScheduleTopNodeId);

    String extractJarPath(String content);

    void deleteJar(Script entity);
}
