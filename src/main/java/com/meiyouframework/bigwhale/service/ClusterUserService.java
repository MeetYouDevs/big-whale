package com.meiyouframework.bigwhale.service;

import com.meiyouframework.bigwhale.data.service.PagingAndSortingQueryService;
import com.meiyouframework.bigwhale.entity.ClusterUser;


public interface ClusterUserService extends PagingAndSortingQueryService<ClusterUser, String> {

    ClusterUser findByUidAndClusterId(String uid, String clusterId);
}
