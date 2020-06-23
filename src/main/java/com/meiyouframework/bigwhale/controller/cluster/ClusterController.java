package com.meiyouframework.bigwhale.controller.cluster;

import com.meiyouframework.bigwhale.entity.Cluster;
import com.meiyouframework.bigwhale.service.ClusterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/cluster")
public class ClusterController {

    @Autowired
    private ClusterService clusterService;

    @RequestMapping(value = "/getall.api", method = RequestMethod.GET)
    public Iterable<Cluster> getAll() {
        return clusterService.findAll();
    }

}
