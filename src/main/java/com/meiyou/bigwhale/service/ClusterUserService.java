package com.meiyou.bigwhale.service;

import com.meiyou.bigwhale.data.service.PagingAndSortingQueryService;
import com.meiyou.bigwhale.entity.ClusterUser;

import java.util.List;


public interface ClusterUserService extends PagingAndSortingQueryService<ClusterUser, Integer> {

    List<ClusterUser> findByClusterIdAndQueue(Integer clusterId, String queue);

}
