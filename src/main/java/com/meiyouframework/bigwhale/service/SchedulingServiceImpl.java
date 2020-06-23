package com.meiyouframework.bigwhale.service;

import com.meiyouframework.bigwhale.data.service.AbstractMysqlPagingAndSortingQueryService;
import com.meiyouframework.bigwhale.entity.Scheduling;
import org.springframework.stereotype.Service;

@Service
public class SchedulingServiceImpl extends AbstractMysqlPagingAndSortingQueryService<Scheduling, String> implements SchedulingService {

}
