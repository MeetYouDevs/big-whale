package com.meiyou.bigwhale.dao;

import com.meiyou.bigwhale.entity.ScheduleSnapshot;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * @author Suxy
 * @date 2020/12/30
 * @description file description
 */
public interface ScheduleSnapshotDao extends PagingAndSortingRepository<ScheduleSnapshot, Integer> {
}
