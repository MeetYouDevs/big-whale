package com.meiyouframework.bigwhale.service.auth;

import com.meiyouframework.bigwhale.data.service.AbstractMysqlPagingAndSortingQueryService;
import com.meiyouframework.bigwhale.entity.auth.User;
import com.meiyouframework.bigwhale.entity.auth.UserRole;
import com.meiyouframework.bigwhale.service.ClusterUserService;
import com.meiyouframework.bigwhale.service.SchedulingService;
import com.meiyouframework.bigwhale.service.ScriptService;
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
public class UserServiceImpl extends AbstractMysqlPagingAndSortingQueryService<User, String> implements UserService {

    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private ClusterUserService clusterUserService;
    @Autowired
    private ScriptService scriptService;
    @Autowired
    private SchedulingService schedulingService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public <S extends User> S save(S entity) {
        //先删除角色
        userRoleService.deleteByQuery("username=" + entity.getUsername());
        if (entity.getRoles() != null && !entity.getRoles().isEmpty()) {
            List<UserRole> userRoles = new ArrayList<>();
            entity.getRoles().forEach(item -> {
                UserRole userRole = new UserRole();
                userRole.setUsername(entity.getUsername());
                userRole.setRole(item);
                userRoles.add(userRole);
            });
            userRoleService.saveAll(userRoles);
        }
        return super.save(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(User entity) {
        userRoleService.deleteByQuery("username=" + entity.getUsername());
        clusterUserService.deleteByQuery("uid=" + entity.getId());
        schedulingService.deleteByQuery("uid=" + entity.getId());
        scriptService.findByQuery("uid=" + entity.getId()).forEach(scriptService::delete);
        super.delete(entity);
    }
}
