package com.meiyou.bigwhale.controller.admin;

import com.alibaba.fastjson.JSON;
import com.meiyou.bigwhale.common.pojo.Msg;
import com.meiyou.bigwhale.data.domain.PageRequest;
import com.meiyou.bigwhale.dto.DtoClusterUser;
import com.meiyou.bigwhale.entity.Cluster;
import com.meiyou.bigwhale.entity.ClusterUser;
import com.meiyou.bigwhale.entity.Script;
import com.meiyou.bigwhale.service.ClusterService;
import com.meiyou.bigwhale.service.ClusterUserService;
import com.meiyou.bigwhale.controller.BaseController;
import com.meiyou.bigwhale.service.ScriptService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/admin/cluster/cluster_user")
public class AdminClusterUserController extends BaseController {

    @Autowired
    private ClusterUserService clusterUserService;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private ScriptService scriptService;

    @RequestMapping(value = "/page.api", method = RequestMethod.POST)
    public Msg page(@RequestBody DtoClusterUser req) {
        List<String> tokens = new ArrayList<>();
        if (StringUtils.isNotBlank(req.getQueue())) {
            tokens.add("queue?" + req.getQueue());
        }
        if (req.getClusterId() != null) {
            tokens.add("clusterId=" + req.getClusterId());
        }
        if (req.getUserId() != null) {
            tokens.add("userId=" + req.getUserId());
        }
        Page<DtoClusterUser> dtoClusterUserPage = clusterUserService.pageByQuery(new PageRequest(req.pageNo - 1, req.pageSize, StringUtils.join(tokens, ";"))).map((item) -> {
            DtoClusterUser dtoClusterUser = new DtoClusterUser();
            BeanUtils.copyProperties(item, dtoClusterUser);
            return dtoClusterUser;
        });
        return success(dtoClusterUserPage);
    }

    @RequestMapping(value = "/save.api", method = RequestMethod.POST)
    public Msg save(@RequestBody DtoClusterUser req) {
        String msg = req.validate();
        if (msg != null) {
            return failed(msg);
        }
        if (req.getId() == null) {
            List<ClusterUser> clusterUsers = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            List<Integer> clusterIds = req.getClusterIds();
            for (Integer item : clusterIds) {
                ClusterUser dbClusterUser = clusterUserService.findOneByQuery("clusterId=" + item + ";userId=" + req.getUserId());
                if (dbClusterUser != null) {
                    Cluster cluster = clusterService.findById(item);
                    errors.add(cluster.getName());
                } else {
                    dbClusterUser = new ClusterUser();
                    BeanUtils.copyProperties(req, dbClusterUser);
                    dbClusterUser.setClusterId(item);
                    clusterUsers.add(dbClusterUser);
                }
            }
            if (errors.isEmpty()) {
                List<DtoClusterUser> dtoClusterUsers = new ArrayList<>();
                clusterUserService.saveAll(clusterUsers).forEach((clusterUser) -> {
                    DtoClusterUser dtoClusterUser = new DtoClusterUser();
                    BeanUtils.copyProperties(clusterUser, dtoClusterUser);
                    dtoClusterUsers.add(dtoClusterUser);
                });
                return success(dtoClusterUsers);
            } else {
                return failed("集群【" + JSON.toJSON(errors) + "】用户重复");
            }
        } else {
            ClusterUser dbClusterUser = clusterUserService.findById(req.getId());
            if (dbClusterUser == null) {
                return failed();
            }
            ClusterUser dupClusterUser = clusterUserService.findOneByQuery("clusterId=" + req.getClusterId() + ";userId=" + req.getUserId() + ";id!=" + req.getId());
            if (dupClusterUser != null) {
                return failed("集群用户重复");
            }
            dbClusterUser = new ClusterUser();
            BeanUtils.copyProperties(req, dbClusterUser);
            clusterUserService.save(dbClusterUser);
            return success(req);
        }
    }

    @RequestMapping(value = "/delete.api", method = RequestMethod.POST)
    public Msg delete(@RequestBody DtoClusterUser req) {
        ClusterUser clusterUser = clusterUserService.findById(req.getId());
        if (clusterUser == null) {
            return failed();
        }
        List<Script> scripts = scriptService.findByQuery("clusterId=" + clusterUser.getClusterId() + ";createBy=" + clusterUser.getUserId());
        if (!scripts.isEmpty()) {
            return failed("集群用户下存在脚本，请先删除");
        }
        clusterUserService.deleteById(req.getId());
        return success();
    }

}
