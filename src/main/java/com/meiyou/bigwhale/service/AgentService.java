package com.meiyou.bigwhale.service;

import com.meiyou.bigwhale.entity.Agent;
import com.meiyou.bigwhale.data.service.PagingAndSortingQueryService;

/**
 * @author progr1mmer
 */
public interface AgentService extends PagingAndSortingQueryService<Agent, Integer> {

    String getInstanceByClusterId(Integer clusterId, boolean check);

    String getInstanceByAgentId(Integer agentId, boolean check);

}
