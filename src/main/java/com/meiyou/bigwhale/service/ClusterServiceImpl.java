package com.meiyou.bigwhale.service;

import com.meiyou.bigwhale.data.service.AbstractMysqlPagingAndSortingQueryService;
import com.meiyou.bigwhale.entity.Cluster;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class ClusterServiceImpl extends AbstractMysqlPagingAndSortingQueryService<Cluster, Integer> implements ClusterService {

    @Autowired
    private ClusterUserService clusterUserService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public <S extends Cluster> S save(S entity) {
        if (entity.getDefaultFileCluster()) {
            findAll().forEach(cluster -> {
                if (cluster.getDefaultFileCluster() && !cluster.getId().equals(entity.getId())) {
                    cluster.setDefaultFileCluster(false);
                    save(cluster);
                }
            });
        }
        return super.save(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Cluster entity) {
        clusterUserService.deleteByQuery("clusterId=" + entity.getId());
        super.delete(entity);
    }

}
