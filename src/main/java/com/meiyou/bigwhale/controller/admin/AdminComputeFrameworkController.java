package com.meiyou.bigwhale.controller.admin;

import com.meiyou.bigwhale.common.pojo.Msg;
import com.meiyou.bigwhale.controller.BaseController;
import com.meiyou.bigwhale.dto.DtoComputeFramework;
import com.meiyou.bigwhale.entity.ComputeFramework;
import com.meiyou.bigwhale.service.ComputeFrameworkService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;


/**
 * @author Suxy
 * @date 2020/2/20
 * @description file description
 */
@RestController
@RequestMapping("/admin/cluster/compute_framework")
public class AdminComputeFrameworkController extends BaseController {

    @Autowired
    private ComputeFrameworkService computeFrameworkService;

    @RequestMapping(value = "/page.api", method = RequestMethod.POST)
    public Msg page(@RequestBody DtoComputeFramework req) {
        Page<DtoComputeFramework> dtoComputeFrameworkPage = computeFrameworkService.findAll(PageRequest.of(req.pageNo - 1, req.pageSize)).map((item) -> {
            DtoComputeFramework dtoComputeFramework = new DtoComputeFramework();
            BeanUtils.copyProperties(item, dtoComputeFramework);
            return dtoComputeFramework;
        });
        return success(dtoComputeFrameworkPage);
    }

    @RequestMapping(value = "/save.api", method = RequestMethod.POST)
    public Msg save(@RequestBody DtoComputeFramework req) {
        String msg = req.validate();
        if (msg != null) {
            return failed(msg);
        }
        if (req.getId() == null) {
            ComputeFramework dupComputeFramework = computeFrameworkService.findOneByQuery("type=" + req.getType() + ";version=" + req.getVersion());
            if (dupComputeFramework != null) {
                return failed("版本重复");
            }
        } else {
            ComputeFramework dbComputeFramework = computeFrameworkService.findById(req.getId());
            if (dbComputeFramework == null) {
                return failed();
            }
            ComputeFramework dupComputeFramework = computeFrameworkService.findOneByQuery("type=" + req.getType() + ";version=" + req.getVersion() + ";id!=" + req.getId());
            if (dupComputeFramework != null) {
                return failed("版本重复");
            }
        }
        ComputeFramework computeFramework = new ComputeFramework();
        BeanUtils.copyProperties(req, computeFramework);
        computeFramework = computeFrameworkService.save(computeFramework);
        if (req.getId() == null) {
            req.setId(computeFramework.getId());
        }
        return success(req);
    }

    @RequestMapping(value = "/delete.api", method = RequestMethod.POST)
    public Msg delete(@RequestBody DtoComputeFramework req) {
        ComputeFramework computeFramework = computeFrameworkService.findById(req.getId());
        if (computeFramework == null) {
            return failed();
        }
        computeFrameworkService.deleteById(req.getId());
        return success();
    }

}
