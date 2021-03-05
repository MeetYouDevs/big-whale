package com.meiyou.bigwhale.service.auth;

import com.meiyou.bigwhale.data.service.AbstractMysqlPagingAndSortingQueryService;
import com.meiyou.bigwhale.entity.auth.Role;
import com.meiyou.bigwhale.entity.auth.RoleResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Suxy
 * @date 2019/10/24
 * @description file description
 */
@Service
public class RoleServiceImpl extends AbstractMysqlPagingAndSortingQueryService<Role, Integer> implements RoleService {

    @Autowired
    private RoleResourceService roleResourceService;
    @Autowired
    private UserRoleService userRoleService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Role role) {
        roleResourceService.deleteByQuery("role=" + role.getCode());
        userRoleService.deleteByQuery("role=" + role.getCode());
        super.delete(role);
    }

    @Override
    public <S extends Role> S save(S entity) {
        //先删除权限
        roleResourceService.deleteByQuery("role=" + entity.getCode());
        if (entity.getResources() != null && !entity.getResources().isEmpty()) {
            List<RoleResource> roleResources = new ArrayList<>();
            entity.getResources().forEach(item -> {
                RoleResource roleResource = new RoleResource();
                roleResource.setRole(entity.getCode());
                roleResource.setResource(item);
                roleResources.add(roleResource);
            });
            roleResourceService.saveAll(roleResources);
        }
        return super.save(entity);
    }
}