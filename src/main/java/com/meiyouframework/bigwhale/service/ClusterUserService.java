package com.meiyouframework.bigwhale.service;

import com.meiyouframework.bigwhale.data.service.PagingAndSortingQueryService;
import com.meiyouframework.bigwhale.entity.ClusterUser;

import java.util.List;


public interface ClusterUserService extends PagingAndSortingQueryService<ClusterUser, String> {

    List<ClusterUser> findByClusterIdAndQueue(String clusterId, String queue);

}
