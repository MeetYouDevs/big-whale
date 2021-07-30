package com.meiyou.bigwhale.service;

import com.meiyou.bigwhale.data.service.PagingAndSortingQueryService;
import com.meiyou.bigwhale.entity.ScriptHistory;

import java.util.Date;

public interface ScriptHistoryService extends PagingAndSortingQueryService<ScriptHistory, Integer> {

    void deleteFuture(Integer scheduleId, Date date);

    void switchScheduleRunnable(Integer id, boolean scheduleRunnable);

    ScriptHistory findScriptLatest(Integer scriptId);

}
