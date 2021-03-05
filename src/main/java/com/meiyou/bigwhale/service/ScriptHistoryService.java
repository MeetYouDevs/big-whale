package com.meiyou.bigwhale.service;

import com.meiyou.bigwhale.data.service.PagingAndSortingQueryService;
import com.meiyou.bigwhale.entity.ScriptHistory;

public interface ScriptHistoryService extends PagingAndSortingQueryService<ScriptHistory, Integer> {

    /**
     * 监控启动
     * @param scriptId
     * @return
     */
    ScriptHistory findNoScheduleLatestByScriptId(Integer scriptId);

    void missingScheduling(ScriptHistory scriptHistory);

    boolean execTimeout(ScriptHistory scriptHistory);

}
