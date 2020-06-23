package com.meiyouframework.bigwhale.controller.cluster;

import com.meiyouframework.bigwhale.controller.BaseController;
import com.meiyouframework.bigwhale.entity.ComputeFramework;
import com.meiyouframework.bigwhale.service.ComputeFrameworkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * @author Suxy
 * @date 2020/2/19
 * @description file description
 */
@RestController
@RequestMapping("/cluster/compute_framework")
public class ComputeFrameworkController extends BaseController {

    @Autowired
    private ComputeFrameworkService computeFrameworkService;

    @RequestMapping(value = "/getall.api")
    public Iterable<ComputeFramework> getAll() {
        return computeFrameworkService.findAll(Sort.by(Sort.Direction.ASC, "orders"));
    }

}
