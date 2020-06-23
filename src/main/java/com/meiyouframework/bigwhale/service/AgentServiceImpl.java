package com.meiyouframework.bigwhale.service;

import com.meiyouframework.bigwhale.data.service.AbstractMysqlPagingAndSortingQueryService;
import com.meiyouframework.bigwhale.entity.Agent;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Random;

@Service
public class AgentServiceImpl extends AbstractMysqlPagingAndSortingQueryService<Agent, String> implements AgentService {

    @Override
    public Agent getByClusterId(String clusterId) {
        List<Agent> agents = findByQuery("status=1;clusterId=" + clusterId);
        if (!CollectionUtils.isEmpty(agents)) {
            int size = agents.size();
            int random = new Random().nextInt(size);
            return agents.get(random);
        }
        return null;
    }

}
