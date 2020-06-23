package com.meiyouframework.bigwhale.service;

import com.meiyouframework.bigwhale.data.service.AbstractMysqlPagingAndSortingQueryService;
import com.meiyouframework.bigwhale.entity.Monitor;

import org.springframework.stereotype.Service;

@Service
public class MonitorServiceImpl extends AbstractMysqlPagingAndSortingQueryService<Monitor, String> implements MonitorService {

}
