package com.meiyouframework.bigwhale.controller.admin.cluster;

import com.meiyouframework.bigwhale.common.pojo.Msg;
import com.meiyouframework.bigwhale.data.domain.PageRequest;
import com.meiyouframework.bigwhale.dto.DtoAgent;
import com.meiyouframework.bigwhale.entity.Agent;
import com.meiyouframework.bigwhale.service.AgentService;
import com.meiyouframework.bigwhale.controller.BaseController;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/admin/cluster/agent")
public class AdminAgentController extends BaseController {

    @Autowired
    private AgentService agentService;

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
        }
        Agent agent = new Agent();
        BeanUtils.copyProperties(req, agent);
        agent = agentService.save(agent);
        return success(agent);
    }

}
