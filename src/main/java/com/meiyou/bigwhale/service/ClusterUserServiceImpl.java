package com.meiyou.bigwhale.service;

import com.meiyou.bigwhale.data.service.AbstractMysqlPagingAndSortingQueryService;
import com.meiyou.bigwhale.entity.ClusterUser;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
public class ClusterUserServiceImpl extends AbstractMysqlPagingAndSortingQueryService<ClusterUser, Integer> implements ClusterUserService {

    @Override
    public List<ClusterUser> findByClusterIdAndQueue(Integer clusterId, String queue) {
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
