package com.meiyouframework.bigwhale.controller.admin.cluster;

import com.meiyouframework.bigwhale.common.Constant;
import com.meiyouframework.bigwhale.common.pojo.Msg;
import com.meiyouframework.bigwhale.data.domain.PageRequest;
import com.meiyouframework.bigwhale.dto.DtoAgent;
import com.meiyouframework.bigwhale.entity.Agent;
import com.meiyouframework.bigwhale.entity.Script;
import com.meiyouframework.bigwhale.service.AgentService;
import com.meiyouframework.bigwhale.controller.BaseController;
import com.meiyouframework.bigwhale.service.ClusterService;
import com.meiyouframework.bigwhale.service.ScriptService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/admin/cluster/agent")
public class AdminAgentController extends BaseController {

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private AgentService agentService;
    @Autowired
    private ScriptService scriptService;

    @RequestMapping(value = "/getpage.api", method = RequestMethod.POST)
    public Page<Agent> getPage(@RequestBody DtoAgent req) {
        List<String> tokens = new ArrayList<>();
        if (StringUtils.isNotBlank(req.getIp())) {
            tokens.add("ip=" + req.getIp());
        }
        if (StringUtils.isNotBlank(req.getHost())) {
            tokens.add("host=" + req.getHost());
        }
        if (StringUtils.isNotBlank(req.getClusterId())) {
            tokens.add("clusterId=" + req.getClusterId());
        }
        return agentService.pageByQuery(new PageRequest(req.pageNo - 1, req.pageSize, StringUtils.join(tokens, ";")));
    }

    @RequestMapping(value = "/save.api", method = RequestMethod.POST)
    public Msg saveOrUpdate(@RequestBody DtoAgent req) {
        String msg = req.validate();
        if (msg != null) {
            return failed(msg);
        }
        if (req.getId() == null) {
            Agent dbAgent = agentService.findOneByQuery("mac=" + req.getMac());
            if (dbAgent != null) {
                return failed("机器重复");
            }
            req.setCreateTime(new Date());
        } else {
            Agent dbAgent = agentService.findById(req.getId());
            if (dbAgent == null) {
                return failed();
            }
            boolean checkFlag = StringUtils.isNotBlank(dbAgent.getClusterId()) &&
                    (req.getStatus() == Constant.STATUS_OFF || !dbAgent.getClusterId().equals(req.getClusterId()));
            if (checkFlag) {
                List<Agent> agents = agentService.findByQuery("status=1;clusterId=" + dbAgent.getClusterId() + ";id!=" + dbAgent.getId());
                if (agents.isEmpty()) {
                    List<Script> scripts = scriptService.findByQuery("clusterId=" + dbAgent.getClusterId());
                    if (!scripts.isEmpty()) {
                        return failed("变更配置前绑定的集群下存在脚本，请先添加新的集群机器");
                    }
                }
            }
        }
        Agent agent = new Agent();
        BeanUtils.copyProperties(req, agent);
        agent = agentService.save(agent);
        return success(agent);
    }

    @RequestMapping(value = "/delete.api", method = RequestMethod.POST)
    public Msg delete(@RequestParam String id) {
        Agent agent = agentService.findById(id);
        if (agent != null) {
            List<Script> scripts = scriptService.findByQuery("agentId=" + agent.getId());
            if (!scripts.isEmpty()) {
                return failed("机器下存在脚本，请先删除");
            }
            if (StringUtils.isNotBlank(agent.getClusterId()) && clusterService.findById(agent.getClusterId()) != null) {
                return failed("机器绑定了集群，请先解绑");
            }
            agentService.deleteById(id);
        }
        return success();
    }

}
