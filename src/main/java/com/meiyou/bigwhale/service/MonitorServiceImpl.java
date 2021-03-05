package com.meiyou.bigwhale.service;

import com.meiyou.bigwhale.data.service.AbstractMysqlPagingAndSortingQueryService;
import com.meiyou.bigwhale.entity.Monitor;
import org.springframework.stereotype.Service;

@Service
public class MonitorServiceImpl extends AbstractMysqlPagingAndSortingQueryService<Monitor, Integer> implements MonitorService {

}
