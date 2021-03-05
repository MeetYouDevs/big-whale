package com.meiyou.bigwhale.service.auth;

import com.meiyou.bigwhale.data.service.AbstractMysqlPagingAndSortingQueryService;
import com.meiyou.bigwhale.entity.auth.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Suxy
 * @date 2019/10/24
 * @description file description
 */
@Service
public class ResourceServiceImpl extends AbstractMysqlPagingAndSortingQueryService<Resource, Integer> implements ResourceService {

    @Autowired
    private RoleResourceService roleResourceService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Resource entity) {
        roleResourceService.deleteByQuery("resource=" + entity.getCode());
        super.delete(entity);
    }
}
