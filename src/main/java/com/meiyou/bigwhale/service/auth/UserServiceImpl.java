package com.meiyou.bigwhale.service.auth;

import com.meiyou.bigwhale.data.service.AbstractMysqlPagingAndSortingQueryService;
import com.meiyou.bigwhale.entity.auth.User;
import com.meiyou.bigwhale.entity.auth.UserRole;
import com.meiyou.bigwhale.service.ClusterUserService;
import com.meiyou.bigwhale.service.MonitorService;
import com.meiyou.bigwhale.service.ScheduleService;
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
public class UserServiceImpl extends AbstractMysqlPagingAndSortingQueryService<User, Integer> implements UserService {

    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private ClusterUserService clusterUserService;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private MonitorService monitorService;

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
        clusterUserService.deleteByQuery("userId=" + entity.getId());
        scheduleService.findByQuery("createBy=" + entity.getId()).forEach(scheduleService::delete);
        monitorService.findByQuery("createBy=" + entity.getId()).forEach(monitorService::delete);
        super.delete(entity);
    }
}
