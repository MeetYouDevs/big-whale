package com.meiyouframework.bigwhale.service;

import com.meiyouframework.bigwhale.data.service.AbstractMysqlPagingAndSortingQueryService;
import com.meiyouframework.bigwhale.entity.ClusterUser;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
public class ClusterUserServiceImpl extends AbstractMysqlPagingAndSortingQueryService<ClusterUser, String> implements ClusterUserService {

    @Override
    public List<ClusterUser> findByClusterIdAndQueue(String clusterId, String queue) {
        List<ClusterUser> clusterUsers = findByQuery("clusterId=" + clusterId + ";queue?" + queue);
        return clusterUsers.stream().filter(clusterUser -> {
            boolean match = false;
            for (String tmpQueue : clusterUser.getQueue().split(",")) {
                if (queue.equals(tmpQueue)) {
                    match = true;
                    break;
                }
            }
            return match;
        }).collect(Collectors.toList());
    }
}
