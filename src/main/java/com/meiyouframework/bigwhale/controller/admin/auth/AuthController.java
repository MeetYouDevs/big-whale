package com.meiyouframework.bigwhale.controller.admin.auth;


import com.meiyouframework.bigwhale.common.Constant;
import com.meiyouframework.bigwhale.common.pojo.Msg;
import com.meiyouframework.bigwhale.entity.auth.*;
import com.meiyouframework.bigwhale.service.SchedulingService;
import com.meiyouframework.bigwhale.controller.BaseController;
import com.meiyouframework.bigwhale.util.SchedulerUtils;
import com.meiyouframework.bigwhale.service.auth.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
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
    private SchedulingService schedulingService;

    private PasswordEncoder passwordEncoder = new StandardPasswordEncoder();

    @RequestMapping(value = "/resource/list.api", method = RequestMethod.GET)
    public Msg getResources() {
        Iterable<Resource> resources = resourceService.findAll();
        return success(resources);
    }

    @RequestMapping(value = "/resource/save.api", method = RequestMethod.POST)
    public Msg saveResource(@RequestBody Resource req) {
        if (req.getUrl() == null) {
            req.setUrl("");
        }
        if (req.getId() == null) {
            Resource dbResource = resourceService.findOneByQuery("code=" + req.getCode());
            if (dbResource != null) {
                return failed("资源已存在");
            }
        }
        req = resourceService.save(req);
        return success(req);
    }

    @RequestMapping(value = "/resource/delete.api", method = RequestMethod.POST)
    public Msg removeResource(String id) {
        Resource resource = resourceService.findById(id);
        if (resource != null) {
            resourceService.delete(resource);
        }
        return success();
    }

    @RequestMapping(value = "/role/list.api", method = RequestMethod.GET)
    public Msg getRoles() {
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
    public Msg saveRole(@RequestBody Role req) {
        if (req.getId() == null) {
            Role dbRole = roleService.findOneByQuery("code=" + req.getCode());
            if (dbRole != null) {
                return failed("角色已存在");
            }
        }
        req = roleService.save(req);
        return success(req);
    }

    @RequestMapping(value = "/role/delete.api", method = RequestMethod.POST)
    public Msg removeRole(String id) {
        Role role = roleService.findById(id);
        if (role != null) {
            roleService.delete(role);
        }
        return success();
    }

    @RequestMapping(value = "/user/list.api", method = RequestMethod.GET)
    public Msg getUsers() {
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

    @RequestMapping(value = "/user/getall.api", method = RequestMethod.GET)
    public Msg getAll() {
        Iterable<User> users = userService.findAll();
        users.forEach(user -> user.setPassword(null));
        return success(users);
    }

    @RequestMapping(value = "/user/save.api", method = RequestMethod.POST)
    public Msg saveUser(@RequestBody User req) {
        Date now = new Date();
        if (req.getId() == null) {
            User dbUser = userService.findOneByQuery("username=" + req.getUsername());
            if (dbUser != null) {
                return failed("用户已存在");
            }
            req.setCreateTime(now);
            req.setPassword(passwordEncoder.encode(req.getPassword()));
        } else {
            User dbUser = userService.findById(req.getId());
            if (dbUser == null) {
                return failed();
            }
            //修改密码
            if (!dbUser.getPassword().equals(req.getPassword())) {
                req.setPassword(passwordEncoder.encode(req.getPassword()));
            }
        }
        req.setUpdateTime(now);
        req = userService.save(req);
        req.setPassword(null);
        return success(req);
    }

    @RequestMapping(value = "/user/delete.api", method = RequestMethod.POST)
    public Msg removeUser(String id) {
        User user = userService.findById(id);
        if (user != null) {
            schedulingService.findByQuery("uid=" + user.getId()).forEach(item -> {
                try {
                    if (item.getType() == Constant.SCHEDULING_TYPE_BATCH) {
                        SchedulerUtils.deleteJob(item.getId(), Constant.JobGroup.TIMED);
                    } else {
                        SchedulerUtils.deleteJob(item.getId(), Constant.JobGroup.MONITOR);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            userService.delete(user);
        }
        return success();
    }

}
