package com.meiyou.bigwhale.controller;

import com.meiyou.bigwhale.common.pojo.Msg;
import com.meiyou.bigwhale.dto.DtoAgent;
import com.meiyou.bigwhale.service.AgentService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/cluster/agent")
public class AgentController extends BaseController {

    @Autowired
    private AgentService agentService;

    @RequestMapping(value = "/all.api", method = RequestMethod.GET)
    public Msg all() {
        List<DtoAgent> dtoAgents = agentService.findByQuery(null).stream().map((item) -> {
            DtoAgent dtoAgent = new DtoAgent();
            BeanUtils.copyProperties(item, dtoAgent);
            return dtoAgent;
        }).collect(Collectors.toList());
        return success(dtoAgents);
    }

}
