package com.meiyouframework.bigwhale.controller.cluster;

import com.meiyouframework.bigwhale.common.pojo.Msg;
import com.meiyouframework.bigwhale.data.domain.PageRequest;
import com.meiyouframework.bigwhale.dto.DtoClusterUser;
import com.meiyouframework.bigwhale.entity.ClusterUser;
import com.meiyouframework.bigwhale.service.ClusterUserService;
import com.meiyouframework.bigwhale.controller.BaseController;
import com.meiyouframework.bigwhale.security.LoginUser;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/cluster/cluster_user")
public class ClusterUserController extends BaseController {

    @Autowired
    private ClusterUserService clusterUserService;

    @RequestMapping(value = "/getpage.api", method = RequestMethod.POST)
    public Msg getPage(@RequestBody DtoClusterUser req) {
        LoginUser user = getCurrentUser();
        if (!user.isRoot()) {
            req.setUid(user.getId());
        }
        List<String> tokens = new ArrayList<>();
        if (StringUtils.isNotBlank(req.getUid())) {
            tokens.add("uid=" + req.getUid());
        }
        if (StringUtils.isNotBlank(req.getClusterId())) {
            tokens.add("clusterId=" + req.getClusterId());
        }
        Page<DtoClusterUser> dtoClusterUserPage = clusterUserService.pageByQuery(new PageRequest(req.pageNo - 1, req.pageSize, StringUtils.join(tokens, ";"))).map((item) -> {
            DtoClusterUser dtoClusterUser = new DtoClusterUser();
            BeanUtils.copyProperties(item, dtoClusterUser);
            return dtoClusterUser;
        });
        return success(dtoClusterUserPage);
    }

    @RequestMapping(value = "/getall.api", method = RequestMethod.GET)
    public Msg getAll() {
        LoginUser user = getCurrentUser();
        List<ClusterUser> clusterUsers;
        if (!user.isRoot()) {
            clusterUsers = clusterUserService.findByQuery("uid=" + user.getId());
        } else {
            clusterUsers = clusterUserService.findByQuery(null);
        }
        List<DtoClusterUser> dtoClusterUsers = clusterUsers.stream().map((item) -> {
            DtoClusterUser dtoClusterUser = new DtoClusterUser();
            BeanUtils.copyProperties(item, dtoClusterUser);
            return dtoClusterUser;
        }).collect(Collectors.toList());
        return success(dtoClusterUsers);
    }

}
