package com.meiyouframework.bigwhale.service;

import com.meiyouframework.bigwhale.data.service.AbstractMysqlPagingAndSortingQueryService;
import com.meiyouframework.bigwhale.entity.Cluster;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class ClusterServiceImpl extends AbstractMysqlPagingAndSortingQueryService<Cluster, String> implements ClusterService {

    @Autowired
    private ClusterUserService clusterUserService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public <S extends Cluster> S save(S entity) {
        if (entity.getDefaultFileCluster()) {
            findByQuery("id!=" + entity.getId()).forEach(item -> {
                if (item.getDefaultFileCluster()) {
                    item.setDefaultFileCluster(false);
                    save(item);
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
