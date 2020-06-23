package com.meiyouframework.bigwhale.service;

import com.meiyouframework.bigwhale.data.service.PagingAndSortingQueryService;
import com.meiyouframework.bigwhale.entity.Agent;


public interface AgentService extends PagingAndSortingQueryService<Agent, String> {

    Agent getByClusterId(String clusterId);

}
