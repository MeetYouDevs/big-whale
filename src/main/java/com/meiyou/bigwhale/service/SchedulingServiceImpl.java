package com.meiyou.bigwhale.service;

import com.meiyou.bigwhale.entity.Scheduling;
import com.meiyou.bigwhale.data.service.AbstractMysqlPagingAndSortingQueryService;
import org.springframework.stereotype.Service;

@Service
public class SchedulingServiceImpl extends AbstractMysqlPagingAndSortingQueryService<Scheduling, String> implements SchedulingService {

}
