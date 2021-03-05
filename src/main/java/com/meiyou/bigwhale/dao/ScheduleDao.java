package com.meiyou.bigwhale.dao;

import com.meiyou.bigwhale.entity.Schedule;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface ScheduleDao extends PagingAndSortingRepository<Schedule, Integer> {

}
