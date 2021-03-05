package com.meiyou.bigwhale.controller.admin;

import com.meiyou.bigwhale.common.pojo.Msg;
import com.meiyou.bigwhale.dto.DtoAgent;
import com.meiyou.bigwhale.entity.Agent;
import com.meiyou.bigwhale.entity.Script;
import com.meiyou.bigwhale.controller.BaseController;
import com.meiyou.bigwhale.data.domain.PageRequest;
import com.meiyou.bigwhale.service.AgentService;
import com.meiyou.bigwhale.service.ScriptService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Suxy
 * @date 2020/7/28
 * @description file description
 */
@RestController
@RequestMapping("/admin/cluster/agent")
public class AdminAgentController extends BaseController {

    @Autowired
    private AgentService agentService;
    @Autowired
    private ScriptService scriptService;

    @RequestMapping(value = "/page.api", method = RequestMethod.POST)
    public Msg page(@RequestBody DtoAgent req) {
        List<String> tokens = new ArrayList<>();
        if (StringUtils.isNotBlank(req.getName())) {
            tokens.add("name?" + req.getName());
        }
        if (req.getClusterId() != null) {
            tokens.add("clusterId=" + req.getClusterId());
        }
        Page<Agent> agentPage = agentService.pageByQuery(new PageRequest(req.pageNo - 1, req.pageSize, StringUtils.join(tokens, ";")));
        Page<DtoAgent> dtoAgentPage = agentPage.map((agent) -> {
            DtoAgent dtoAgent = new DtoAgent();
            BeanUtils.copyProperties(agent, dtoAgent);
            List<String> instances = new ArrayList<>();
            Collections.addAll(instances, agent.getInstances().split(","));
            dtoAgent.setInstances(instances);
            return dtoAgent;
        });
        return success(dtoAgentPage);
    }

    @RequestMapping(value = "/save.api", method = RequestMethod.POST)
    public Msg save(@RequestBody DtoAgent req) {
        String msg = req.validate();
        if (msg != null) {
            return failed(msg);
        }
        if (req.getId() == null) {
            Agent dupAgent = agentService.findOneByQuery("name=" + req.getName());
            if (dupAgent != null) {
                return failed("代理重复");
            }
        } else {
            Agent dbAgent = agentService.findById(req.getId());
            if (dbAgent == null) {
                return failed();
            }
            Agent dupAgent = agentService.findOneByQuery("name=" + req.getName() + ";id!=" + req.getId());
            if (dupAgent != null) {
                return failed("代理重复");
            }
            boolean checkFlag = dbAgent.getClusterId() != null && !dbAgent.getClusterId().equals(req.getClusterId());
            if (checkFlag) {
                List<Agent> agents = agentService.findByQuery("clusterId=" + dbAgent.getClusterId() + ";id!=" + dbAgent.getId());
                if (agents.isEmpty()) {
                    List<Script> scripts = scriptService.findByQuery("clusterId=" + dbAgent.getClusterId());
                    if (!scripts.isEmpty()) {
                        return failed("变更配置前绑定的集群下存在脚本，请先添加新的集群代理");
                    }
                }
            }
        }
        Agent agent = new Agent();
        BeanUtils.copyProperties(req, agent);
        agent.setInstances(StringUtils.join(req.getInstances(), ","));
        agent = agentService.save(agent);
        if (req.getId() == null) {
            req.setId(agent.getId());
        }
        return success(req);
    }

    @RequestMapping(value = "/delete.api", method = RequestMethod.POST)
    public Msg delete(@RequestBody DtoAgent req) {
        Agent agent = agentService.findById(req.getId());
        if (agent == null) {
            return failed();
        }
        if (agent.getClusterId() != null) {
            return failed("代理绑定了集群，请先解绑");
        }
        List<Script> scripts = scriptService.findByQuery("agentId=" + agent.getId());
        if (!scripts.isEmpty()) {
            return failed("代理下存在脚本，请先删除");
        }
        agentService.deleteById(req.getId());
        return success();
    }

}
