package com.meiyou.bigwhale.service;

import ch.ethz.ssh2.Connection;
import com.meiyou.bigwhale.entity.Agent;
import com.meiyou.bigwhale.config.SshConfig;
import com.meiyou.bigwhale.data.service.AbstractMysqlPagingAndSortingQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author progr1mmer
 */
@Service
public class AgentServiceImpl extends AbstractMysqlPagingAndSortingQueryService<Agent, Integer> implements AgentService {

    private Logger logger = LoggerFactory.getLogger(AgentServiceImpl.class);

    @Autowired
    private SshConfig sshConfig;

    @Override
    public String getInstanceByClusterId(Integer clusterId, boolean check) {
        List<Agent> agents = findByQuery("clusterId=" + clusterId);
        while (!agents.isEmpty()) {
            int random = new Random().nextInt(agents.size());
            Agent agent = agents.get(random);
            try {
                return getInstanceByAgentId(agent.getId(), check);
            } catch (IllegalStateException e) {
                agents.remove(agent);
            }
        }
        throw new IllegalStateException("No agent instance accessible");
    }

    @Override
    public String getInstanceByAgentId(Integer agentId, boolean check) {
        Agent agent = findById(agentId);
        List<String> instances = new ArrayList<>();
        Collections.addAll(instances, agent.getInstances().split(","));
        while (!instances.isEmpty()) {
            int random = new Random().nextInt(instances.size());
            String instance = instances.get(random);
            if (check) {
                if (isAccessible(instance)) {
                    return instance;
                } else {
                    instances.remove(instance);
                }
            } else {
                return instance;
            }
        }
        throw new IllegalStateException("No agent instance accessible");
    }

    private boolean isAccessible(String instance) {
        Connection conn = null;
        try {
            if (instance.contains(":")) {
                String [] arr = instance.split(":");
                conn = new Connection(arr[0], Integer.parseInt(arr[1]));
            } else {
                conn = new Connection(instance);
            }
            conn.connect(null, sshConfig.getConnectTimeout(), 30000);
            boolean isAuthenticated = conn.authenticateWithPassword(sshConfig.getUser(), sshConfig.getPassword());
            if (!isAuthenticated) {
                throw new IllegalArgumentException("Incorrect username or password");
            }
            return true;
        } catch (Exception e) {
            logger.warn(e.getMessage());
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
        return false;
    }

}
