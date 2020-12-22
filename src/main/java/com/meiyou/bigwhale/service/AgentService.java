package com.meiyou.bigwhale.service;

import com.meiyou.bigwhale.entity.Agent;
import com.meiyou.bigwhale.data.service.PagingAndSortingQueryService;

/**
 * @author progr1mmer
 */
public interface AgentService extends PagingAndSortingQueryService<Agent, String> {

    String getInstanceByClusterId(String clusterId, boolean check);

    String getInstanceByAgentId(String agentId, boolean check);

}
