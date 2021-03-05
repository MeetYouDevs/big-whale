package com.meiyou.bigwhale.service;

import com.meiyou.bigwhale.data.service.PagingAndSortingQueryService;
import com.meiyou.bigwhale.dto.DtoScript;
import com.meiyou.bigwhale.entity.Monitor;
import com.meiyou.bigwhale.entity.Script;
import com.meiyou.bigwhale.entity.ScriptHistory;
import com.meiyou.bigwhale.entity.ScheduleSnapshot;
import org.springframework.data.domain.Page;

public interface ScriptService extends PagingAndSortingQueryService<Script, Integer> {

    Page<Script> fuzzyPage(DtoScript req);

    String validate(DtoScript req);

    void update(Script entity, Monitor monitorEntity);

    boolean execute(Script script, Monitor monitor);

    ScriptHistory generateHistory(Script script);

    /**
     *
     * @param script
     * @param scheduleSnapshot
     * @param scheduleInstanceId
     * @param generateStatus
     * @return 0 需确认 1 确认 2补数
     */
    ScriptHistory generateHistory(Script script, ScheduleSnapshot scheduleSnapshot, String scheduleInstanceId, int generateStatus);

    String extractJarPath(String content);

    void deleteJar(Script entity);
}
