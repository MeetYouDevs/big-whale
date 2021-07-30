package com.meiyou.bigwhale.service;

import com.meiyou.bigwhale.dto.DtoSchedule;
import com.meiyou.bigwhale.entity.Script;
import com.meiyou.bigwhale.entity.Schedule;
import com.meiyou.bigwhale.data.service.PagingAndSortingQueryService;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ScheduleService extends PagingAndSortingQueryService<Schedule, Integer> {

    Schedule update(Schedule entity, List<Script> scriptEntities);

    Page<String> instancePage(DtoSchedule req);
}
