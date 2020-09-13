package com.meiyouframework.bigwhale.controller.admin.cluster;

import com.meiyouframework.bigwhale.common.pojo.Msg;
import com.meiyouframework.bigwhale.controller.BaseController;
import com.meiyouframework.bigwhale.data.domain.PageRequest;
import com.meiyouframework.bigwhale.dto.DtoAgent;
import com.meiyouframework.bigwhale.entity.Agent;
import com.meiyouframework.bigwhale.entity.Script;
import com.meiyouframework.bigwhale.service.AgentService;
import com.meiyouframework.bigwhale.service.ScriptService;
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

    @RequestMapping(value = "/getpage.api", method = RequestMethod.POST)
    public Msg getPage(@RequestBody DtoAgent req) {
        List<String> tokens = new ArrayList<>();
        if (StringUtils.isNotBlank(req.getName())) {
            tokens.add("name?" + req.getName());
        }
        if (StringUtils.isNotBlank(req.getClusterId())) {
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
    public Msg saveOrUpdate(@RequestBody DtoAgent req) {
        String msg = req.validate();
        if (msg != null) {
            return failed(msg);
        }
        if (req.getId() == null) {
            Agent dbAgent = agentService.findOneByQuery("name=" + req.getName());
            if (dbAgent != null) {
                return failed("代理重复");
            }
        } else {
            Agent dbAgent = agentService.findById(req.getId());
            if (dbAgent == null) {
                return failed();
            }
            boolean checkFlag = StringUtils.isNotBlank(dbAgent.getClusterId()) && !dbAgent.getClusterId().equals(req.getClusterId());
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
    public Msg delete(@RequestParam String id) {
        Agent agent = agentService.findById(id);
        if (agent != null) {
            List<Script> scripts = scriptService.findByQuery("agentId=" + agent.getId());
            if (!scripts.isEmpty()) {
                return failed("代理下存在脚本，请先删除");
            }
            if (StringUtils.isNotBlank(agent.getClusterId())) {
                return failed("代理绑定了集群，请先解绑");
            }
            agentService.deleteById(id);
        }
        return success();
    }

}
