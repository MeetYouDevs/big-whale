package com.meiyouframework.bigwhale.controller.cluster;

import com.meiyouframework.bigwhale.controller.BaseController;
import com.meiyouframework.bigwhale.entity.Agent;
import com.meiyouframework.bigwhale.service.AgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cluster/agent")
public class AgentController extends BaseController {

    @Autowired
    private AgentService agentService;

    @RequestMapping(value = "/getall.api", method = RequestMethod.GET)
    public Iterable<Agent> getAll() {
        return agentService.findAll();
    }

}
