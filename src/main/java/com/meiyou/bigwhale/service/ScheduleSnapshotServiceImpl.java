package com.meiyou.bigwhale.service;

import com.meiyou.bigwhale.data.service.AbstractMysqlPagingAndSortingQueryService;
import com.meiyou.bigwhale.entity.ScheduleSnapshot;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Suxy
 * @date 2020/12/30
 * @description file description
 */
@Service
public class ScheduleSnapshotServiceImpl extends AbstractMysqlPagingAndSortingQueryService<ScheduleSnapshot, Integer> implements ScheduleSnapshotService {

    @Override
    public ScheduleSnapshot findByScheduleIdAndSnapshotTime(Integer scheduleId, Date snapshotTime) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return findOneByQuery("scheduleId=" + scheduleId + ";snapshotTime<=" + dateFormat.format(snapshotTime), new Sort(Sort.Direction.DESC, "snapshotTime"));
    }

}
