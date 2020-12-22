package com.meiyou.bigwhale.controller.yarnapp;

import com.meiyou.bigwhale.common.pojo.Msg;
import com.meiyou.bigwhale.controller.BaseController;
import com.meiyou.bigwhale.data.domain.PageRequest;
import com.meiyou.bigwhale.dto.DtoYarnApp;
import com.meiyou.bigwhale.entity.Cluster;
import com.meiyou.bigwhale.entity.YarnApp;
import com.meiyou.bigwhale.security.LoginUser;
import com.meiyou.bigwhale.service.ClusterService;
import com.meiyou.bigwhale.service.YarnAppService;
import com.meiyou.bigwhale.util.YarnApiUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/yarn_app")
public class YarnAppController extends BaseController {

    @Autowired
    private YarnAppService yarnAppService;
    @Autowired
    private ClusterService clusterService;

    @RequestMapping(value = "/getpage.api", method = RequestMethod.POST)
    public Msg getPage(@RequestBody DtoYarnApp req) {
        LoginUser user = getCurrentUser();
        if (!user.isRoot()) {
            req.setUid(user.getId());
        }
        List<String> tokens = new ArrayList<>();
        if (StringUtils.isNotBlank(req.getName())) {
            tokens.add("name?" + req.getName());
        }
        if (StringUtils.isNotBlank(req.getAppId())) {
            tokens.add("appId=" + req.getAppId());
        }
        if (StringUtils.isNotBlank(req.getUid())) {
            tokens.add("uid=" + req.getUid());
        }
        if (StringUtils.isNotBlank(req.getClusterId())) {
            tokens.add("clusterId=" + req.getClusterId());
        }
        Page<DtoYarnApp> dtoYarnAppPage = yarnAppService.pageByQuery(new PageRequest(req.pageNo - 1, req.pageSize, StringUtils.join(tokens, ";"))).map(item -> {
            DtoYarnApp dtoYarnApp = new DtoYarnApp();
            BeanUtils.copyProperties(item, dtoYarnApp);
            return dtoYarnApp;
        });
        return success(dtoYarnAppPage);
    }

    @RequestMapping(value = "/kill.api", method = RequestMethod.POST)
    public Msg kill(@RequestParam String appId) {
        YarnApp appInfo = yarnAppService.findOneByQuery("appId=" + appId);
        if (appInfo == null) {
            return failed();
        }
        Cluster cluster = clusterService.findById(appInfo.getClusterId());
        boolean success = YarnApiUtils.killApp(cluster.getYarnUrl(), appInfo.getAppId());
        if (success) {
            yarnAppService.deleteByQuery("appId=" + appId);
            return success();
        }
        return failed();
    }

}
