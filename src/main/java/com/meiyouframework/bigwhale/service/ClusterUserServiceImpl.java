package com.meiyouframework.bigwhale.service;

import com.meiyouframework.bigwhale.dao.ClusterUserDao;
import com.meiyouframework.bigwhale.data.service.AbstractMysqlPagingAndSortingQueryService;
import com.meiyouframework.bigwhale.entity.ClusterUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class ClusterUserServiceImpl extends AbstractMysqlPagingAndSortingQueryService<ClusterUser, String> implements ClusterUserService {

    @Autowired
    private ClusterUserDao clusterUserDao;

    @Override
    public ClusterUser findByUidAndClusterId(String uid, String clusterId) {
        return clusterUserDao.findByUidAndClusterId(uid, clusterId);
    }
}
