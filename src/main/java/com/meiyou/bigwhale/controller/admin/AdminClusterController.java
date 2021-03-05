package com.meiyou.bigwhale.controller.admin;

import com.meiyou.bigwhale.common.pojo.Msg;
import com.meiyou.bigwhale.controller.BaseController;
import com.meiyou.bigwhale.dto.DtoCluster;
import com.meiyou.bigwhale.entity.Agent;
import com.meiyou.bigwhale.entity.Cluster;
import com.meiyou.bigwhale.entity.Script;
import com.meiyou.bigwhale.service.AgentService;
import com.meiyou.bigwhale.service.ClusterService;
import com.meiyou.bigwhale.service.ScriptService;
import com.meiyou.bigwhale.util.WebHdfsUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/admin/cluster")
public class AdminClusterController extends BaseController {

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private AgentService agentService;
    @Autowired
    private ScriptService scriptService;

    @RequestMapping(value = "/page.api", method = RequestMethod.POST)
    public Msg page(@RequestBody DtoCluster req) {
        Page<DtoCluster> dtoClusterPage = clusterService.findAll(PageRequest.of(req.pageNo - 1, req.pageSize)).map((item) -> {
            DtoCluster dtoCluster = new DtoCluster();
            BeanUtils.copyProperties(item, dtoCluster);
            return dtoCluster;
        });
        return success(dtoClusterPage);
    }

    @RequestMapping(value = "/save.api", method = RequestMethod.POST)
    public Msg save(@RequestBody DtoCluster req) {
        String msg = req.validate();
        if (msg != null) {
            return failed(msg);
        }
        if (req.getId() == null) {
            Cluster dupCluster = clusterService.findOneByQuery("name=" + req.getName());
            if (dupCluster != null) {
                return failed( "集群重复");
            }
        } else {
            Cluster dbCluster = clusterService.findById(req.getId());
            if (dbCluster == null) {
                return failed();
            }
            Cluster dupCluster = clusterService.findOneByQuery("name=" + req.getName() + ";id!=" + req.getId());
            if (dupCluster != null) {
                return failed( "集群重复");
            }
        }
        //创建目录
        boolean success = WebHdfsUtils.mkdir(req.getFsWebhdfs(), req.getFsDir(), req.getFsUser(), req.getFsUser());
        if (!success) {
            return failed("程序包存储目录创建失败");
        }
        Cluster cluster = new Cluster();
        BeanUtils.copyProperties(req, cluster);
        cluster = clusterService.save(cluster);
        if (req.getId() == null) {
            req.setId(cluster.getId());
        }
        return success(req);
    }

    @RequestMapping(value = "/delete.api", method = RequestMethod.POST)
    public Msg delete(@RequestBody DtoCluster req) {
        Cluster cluster = clusterService.findById(req.getId());
        if (cluster == null) {
            return failed();
        }
        List<Agent> agents = agentService.findByQuery("clusterId=" + cluster.getId());
        if (!agents.isEmpty()) {
            return failed("集群下存在代理，请先删除");
        }
        List<Script> scripts = scriptService.findByQuery("clusterId=" + cluster.getId());
        if (!scripts.isEmpty()) {
            return failed("集群下存在脚本，请先删除");
        }
        clusterService.delete(cluster);
        return success();
    }
}
