package com.meiyouframework.bigwhale.controller.cluster;

import com.meiyouframework.bigwhale.common.pojo.Msg;
import com.meiyouframework.bigwhale.controller.BaseController;
import com.meiyouframework.bigwhale.dto.DtoAgent;
import com.meiyouframework.bigwhale.service.AgentService;
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

    @RequestMapping(value = "/getall.api", method = RequestMethod.GET)
    public Msg getAll() {
        List<DtoAgent> dtoAgents = agentService.findByQuery(null).stream().map((item) -> {
            DtoAgent dtoAgent = new DtoAgent();
            BeanUtils.copyProperties(item, dtoAgent);
            return dtoAgent;
        }).collect(Collectors.toList());
        return success(dtoAgents);
    }

}
