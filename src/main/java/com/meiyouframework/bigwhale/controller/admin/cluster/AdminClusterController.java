package com.meiyouframework.bigwhale.controller.admin.cluster;

import com.meiyouframework.bigwhale.common.Constant;
import com.meiyouframework.bigwhale.common.pojo.Msg;
import com.meiyouframework.bigwhale.dto.DtoCluster;
import com.meiyouframework.bigwhale.entity.Cluster;
import com.meiyouframework.bigwhale.service.ClusterService;
import com.meiyouframework.bigwhale.service.MonitorService;
import com.meiyouframework.bigwhale.service.ScriptService;
import com.meiyouframework.bigwhale.service.SchedulingService;
import com.meiyouframework.bigwhale.controller.BaseController;
import com.meiyouframework.bigwhale.util.SchedulerUtils;
import com.meiyouframework.bigwhale.util.WebHdfsUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/admin/cluster")
public class AdminClusterController extends BaseController {

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private ScriptService scriptService;
    @Autowired
    private MonitorService monitorService;
    @Autowired
    private SchedulingService schedulingService;


    @RequestMapping(value = "/getpage.api", method = RequestMethod.POST)
    public Page<Cluster> getPage(@RequestBody DtoCluster req) {
        return clusterService.findAll(PageRequest.of(req.pageNo - 1, req.pageSize));
    }

    @RequestMapping(value = "/save.api", method = RequestMethod.POST)
    public Msg saveOrUpdate(@RequestBody DtoCluster req) {
        String msg = req.validate();
        if (msg != null) {
            return failed(msg);
        }
        if (req.getId() == null) {
            Cluster dbCluster = clusterService.findOneByQuery("name=" + req.getName());
            if (dbCluster != null) {
                return failed( "集群重复");
            }
        } else {
            Cluster dbCluster = clusterService.findById(req.getId());
            if (dbCluster == null) {
                return failed();
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
        return success(cluster);
    }

    @RequestMapping(value = "/delete.api", method = RequestMethod.POST)
    public Msg delete(@RequestParam String id) {
        Cluster cluster = clusterService.findById(id);
        if (cluster != null) {
            scriptService.findByQuery("clusterId=" + cluster.getId()).forEach(scriptInfo -> {
                monitorService.findByQuery("scriptId=" + scriptInfo.getId()).forEach(item -> {
                    try {
                        SchedulerUtils.deleteJob(item.getId(), Constant.JobGroup.MONITOR);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                schedulingService.findByQuery("scriptId=" + scriptInfo.getId()).forEach(item -> {
                    try {
                        SchedulerUtils.deleteJob(item.getId(), Constant.JobGroup.TIMED);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            });
            clusterService.delete(cluster);
        }
        return success();
    }
}
