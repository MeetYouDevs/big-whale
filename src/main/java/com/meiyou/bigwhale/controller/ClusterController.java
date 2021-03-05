package com.meiyou.bigwhale.controller;

import com.meiyou.bigwhale.common.pojo.Msg;
import com.meiyou.bigwhale.dto.DtoCluster;
import com.meiyou.bigwhale.service.ClusterService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/cluster")
public class ClusterController extends BaseController {

    @Autowired
    private ClusterService clusterService;

    @RequestMapping(value = "/all.api", method = RequestMethod.GET)
    public Msg all() {
        List<DtoCluster> dtoClusters = clusterService.findByQuery(null).stream().map((item) -> {
            DtoCluster dtoCluster = new DtoCluster();
            BeanUtils.copyProperties(item, dtoCluster);
            return dtoCluster;
        }).collect(Collectors.toList());
        return success(dtoClusters);
    }

}
