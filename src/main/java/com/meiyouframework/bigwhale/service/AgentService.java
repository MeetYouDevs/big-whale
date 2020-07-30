package com.meiyouframework.bigwhale.service;

import com.meiyouframework.bigwhale.data.service.PagingAndSortingQueryService;
import com.meiyouframework.bigwhale.entity.Agent;

/**
 * @author progr1mmer
 */
public interface AgentService extends PagingAndSortingQueryService<Agent, String> {

    String getInstanceByClusterId(String clusterId, boolean check);

    String getInstanceByAgentId(String agentId, boolean check);

}
