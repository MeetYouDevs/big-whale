package com.meiyou.bigwhale.service;

import com.meiyou.bigwhale.data.service.PagingAndSortingQueryService;
import com.meiyou.bigwhale.entity.ScheduleSnapshot;

import java.util.Date;

/**
 * @author Suxy
 * @date 2020/12/30
 * @description file description
 */
public interface ScheduleSnapshotService extends PagingAndSortingQueryService<ScheduleSnapshot, Integer> {

    /**
     * 获取最近的调度快照
     * @param scheduleId
     * @param snapshotTime
     * @return
     */
    ScheduleSnapshot findByScheduleIdAndSnapshotTime(Integer scheduleId, Date snapshotTime);
}
