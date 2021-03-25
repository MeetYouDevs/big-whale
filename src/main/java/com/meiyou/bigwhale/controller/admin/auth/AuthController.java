package com.meiyou.bigwhale.controller.admin.auth;

import com.meiyou.bigwhale.common.Constant;
import com.meiyou.bigwhale.common.pojo.Msg;
import com.meiyou.bigwhale.controller.BaseController;
import com.meiyou.bigwhale.entity.auth.*;
import com.meiyou.bigwhale.security.LoginUser;
import com.meiyou.bigwhale.service.MonitorService;
import com.meiyou.bigwhale.service.ScheduleService;
import com.meiyou.bigwhale.service.auth.*;
import com.meiyou.bigwhale.util.SchedulerUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * @author Suxy
 * @date 2019/9/25
 * @description file description
 */
@RestController
@RequestMapping("/auth")
public class AuthController extends BaseController {

    @Autowired
    private ResourceService resourceService;
    @Autowired
    private RoleResourceService roleResourceService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private UserService userService;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private MonitorService monitorService;

    @RequestMapping(value = "/resource/list.api", method = RequestMethod.GET)
    public Msg resourceList() {
        Iterable<Resource> resources = resourceService.findAll();
        return success(resources);
    }

    @RequestMapping(value = "/resource/save.api", method = RequestMethod.POST)
    public Msg resourceSave(@RequestBody Resource req) {
        if (req.getUrl() == null) {
            req.setUrl("");
        }
        if (req.getId() == null) {
            Resource dupResource = resourceService.findOneByQuery("code=" + req.getCode());
            if (dupResource != null) {
                return failed("资源已存在");
            }
        } else {
            Resource dbResource = resourceService.findById(req.getId());
            if (dbResource == null) {
                return failed();
            }
        }
        req = resourceService.save(req);
        return success(req);
    }

    @RequestMapping(value = "/resource/delete.api", method = RequestMethod.POST)
    public Msg resourceDelete(@RequestBody Resource req) {
        Resource resource = resourceService.findById(req.getId());
        if (resource == null) {
            return failed();
        }
        resourceService.delete(resource);
        return success();
    }

    @RequestMapping(value = "/role/list.api", method = RequestMethod.GET)
    public Msg roleList() {
        Iterable<Role> roles = roleService.findAll();
        roles.forEach(role -> {
            List<RoleResource> roleResources = roleResourceService.findByQuery("role=" + role.getCode());
            List<String> resources = new ArrayList<>();
            if (!roleResources.isEmpty()) {
                roleResources.forEach(roleResource -> resources.add(roleResource.getResource()));
            }
            role.setResources(resources);
        });
        return success(roles);
    }

    @RequestMapping(value = "/role/save.api", method = RequestMethod.POST)
    public Msg roleSave(@RequestBody Role req) {
        if (req.getId() == null) {
            Role dupRole = roleService.findOneByQuery("code=" + req.getCode());
            if (dupRole != null) {
                return failed("角色已存在");
            }
        } else {
            Role dbRole = roleService.findById(req.getId());
            if (dbRole == null) {
                return failed();
            }
        }
        req = roleService.save(req);
        return success(req);
    }

    @RequestMapping(value = "/role/delete.api", method = RequestMethod.POST)
    public Msg roleDelete(@RequestBody Role req) {
        Role role = roleService.findById(req.getId());
        if (role == null) {
            return failed();
        }
        roleService.delete(role);
        return success();
    }

    @RequestMapping(value = "/user/list.api", method = RequestMethod.GET)
    public Msg userList() {
        Iterable<User> users = userService.findAll();
        users.forEach(user -> {
            List<UserRole> userRoles = userRoleService.findByQuery("username=" + user.getUsername());
            List<String> roles = new ArrayList<>();
            if (!userRoles.isEmpty()) {
                userRoles.forEach(userRole -> roles.add(userRole.getRole()));
            }
            user.setRoles(roles);
        });
        return success(users);
    }

    @RequestMapping(value = "/user/all.api", method = RequestMethod.GET)
    public Msg userAll() {
        Iterable<User> users = userService.findAll();
        users.forEach(user -> user.setPassword(null));
        return success(users);
    }

    @RequestMapping(value = "/user/save.api", method = RequestMethod.POST)
    public Msg userSave(@RequestBody User req) {
        Date now = new Date();
        if (req.getId() == null) {
            User dupUser = userService.findOneByQuery("username=" + req.getUsername());
            if (dupUser != null) {
                return failed("用户已存在");
            }
            req.setCreateTime(now);
            req.setPassword(LoginUser.PASSWORD_ENCODER.encode(req.getPassword()));
        } else {
            User dbUser = userService.findById(req.getId());
            if (dbUser == null) {
                return failed();
            }
            //修改密码
            if (!dbUser.getPassword().equals(req.getPassword())) {
                req.setPassword(LoginUser.PASSWORD_ENCODER.encode(req.getPassword()));
            }
        }
        req.setUpdateTime(now);
        req = userService.save(req);
        req.setPassword(null);
        return success(req);
    }

    @RequestMapping(value = "/user/delete.api", method = RequestMethod.POST)
    public Msg userDelete(@RequestBody User req) {
        User user = userService.findById(req.getId());
        if (user == null) {
            return failed();
        }
        scheduleService.findByQuery("createBy=" + user.getId()).forEach(schedule -> SchedulerUtils.deleteJob(schedule.getId(), Constant.JobGroup.SCHEDULE));
        monitorService.findByQuery("createBy=" + user.getId()).forEach(monitor -> SchedulerUtils.deleteJob(monitor.getId(), Constant.JobGroup.MONITOR));
        userService.delete(user);
        return success();
    }

}
