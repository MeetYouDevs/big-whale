package com.meiyou.bigwhale.controller;

import com.meiyou.bigwhale.common.pojo.Msg;
import com.meiyou.bigwhale.dto.DtoComputeFramework;
import com.meiyou.bigwhale.service.ComputeFrameworkService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;


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

    @RequestMapping(value = "/all.api", method = RequestMethod.GET)
    public Msg all() {
        List<DtoComputeFramework> dtoComputeFrameworks = computeFrameworkService.findByQuery(null, Sort.by(Sort.Direction.ASC, "orders")).stream().map((item) -> {
            DtoComputeFramework dtoComputeFramework = new DtoComputeFramework();
            BeanUtils.copyProperties(item, dtoComputeFramework);
            return dtoComputeFramework;
        }).collect(Collectors.toList());
        return success(dtoComputeFrameworks);
    }

}
