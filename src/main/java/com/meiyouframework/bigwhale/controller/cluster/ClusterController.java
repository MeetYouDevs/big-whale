package com.meiyouframework.bigwhale.controller.cluster;

import com.meiyouframework.bigwhale.common.pojo.Msg;
import com.meiyouframework.bigwhale.controller.BaseController;
import com.meiyouframework.bigwhale.dto.DtoCluster;
import com.meiyouframework.bigwhale.service.ClusterService;
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

    @RequestMapping(value = "/getall.api", method = RequestMethod.GET)
    public Msg getAll() {
        List<DtoCluster> dtoClusters = clusterService.findByQuery(null).stream().map((item) -> {
            DtoCluster dtoCluster = new DtoCluster();
            BeanUtils.copyProperties(item, dtoCluster);
            return dtoCluster;
        }).collect(Collectors.toList());
        return success(dtoClusters);
    }

}
