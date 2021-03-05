package com.meiyou.bigwhale.controller;

import com.meiyou.bigwhale.common.pojo.Msg;
import com.meiyou.bigwhale.dto.DtoClusterUser;
import com.meiyou.bigwhale.entity.ClusterUser;
import com.meiyou.bigwhale.service.ClusterUserService;
import com.meiyou.bigwhale.security.LoginUser;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/cluster/cluster_user")
public class ClusterUserController extends BaseController {

    @Autowired
    private ClusterUserService clusterUserService;

    @RequestMapping(value = "/all.api", method = RequestMethod.GET)
    public Msg all() {
        LoginUser currentUser = getCurrentUser();
        List<ClusterUser> clusterUsers;
        if (!currentUser.isRoot()) {
            clusterUsers = clusterUserService.findByQuery("userId=" + currentUser.getId());
        } else {
            clusterUsers = clusterUserService.findByQuery(null);
        }
        List<DtoClusterUser> dtoClusterUsers = clusterUsers.stream().map((clusterUser) -> {
            DtoClusterUser dtoClusterUser = new DtoClusterUser();
            BeanUtils.copyProperties(clusterUser, dtoClusterUser);
            return dtoClusterUser;
        }).collect(Collectors.toList());
        return success(dtoClusterUsers);
    }

    @RequestMapping(value = "/find_one.api", method = RequestMethod.GET)
    public Msg findOne(@RequestParam String clusterId,
                       @RequestParam String userId) {
        ClusterUser clusterUser = clusterUserService.findOneByQuery("clusterId=" + clusterId + ";userId=" + userId);
        return success(clusterUser);
    }

}
