package com.meiyouframework.bigwhale.controller.monitor;

import com.meiyouframework.bigwhale.common.Constant;
import com.meiyouframework.bigwhale.common.pojo.Msg;
import com.meiyouframework.bigwhale.data.domain.PageRequest;
import com.meiyouframework.bigwhale.dto.DtoMonitor;
import com.meiyouframework.bigwhale.entity.Monitor;
import com.meiyouframework.bigwhale.service.MonitorService;
import com.meiyouframework.bigwhale.controller.BaseController;
import com.meiyouframework.bigwhale.security.LoginUser;
import com.meiyouframework.bigwhale.task.monitor.AbstractMonitorRunner;
import com.meiyouframework.bigwhale.util.SchedulerUtils;
import org.apache.commons.lang.StringUtils;
import org.quartz.SchedulerException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/monitor")
public class MonitorController extends BaseController {

    @Autowired
    private MonitorService monitorService;

    @RequestMapping(value = "/getpage.api", method = RequestMethod.POST)
    public Page<DtoMonitor> getPage(@RequestBody DtoMonitor req) {
        LoginUser user = getCurrentUser();
        if (!user.isRoot()) {
            req.setUid(user.getId());
        }
        List<String> tokens = new ArrayList<>();
        if (StringUtils.isNotBlank(req.getUid())) {
            tokens.add("uid=" + req.getUid());
        }
        if (StringUtils.isNotBlank(req.getScriptId())) {
            tokens.add("scriptId=" + req.getScriptId());
        }
        if (req.getType() != null) {
            tokens.add("type=" + req.getType());
        }
        Page<Monitor> pages = monitorService.pageByQuery(new PageRequest(req.pageNo - 1, req.pageSize, StringUtils.join(tokens, ";")));
        return pages.map((monitorInfo) -> {
            DtoMonitor dtoMonitor = new DtoMonitor();
            BeanUtils.copyProperties(monitorInfo, dtoMonitor);
            if (StringUtils.isNotBlank(monitorInfo.getDingdingHooks())) {
                List<String> dingdingHooks = new ArrayList<>();
                Collections.addAll(dingdingHooks, monitorInfo.getDingdingHooks().split(","));
                dtoMonitor.setDingdingHooks(dingdingHooks);
            }
            return dtoMonitor;
        });
    }

    @RequestMapping(value = "/getall.api", method = RequestMethod.GET)
    public Iterable<DtoMonitor> getAll() {
        LoginUser user = getCurrentUser();
        Iterable<Monitor> monitorInfos;
        if (!user.isRoot()) {
            monitorInfos = monitorService.findByQuery("uid=" + user.getId());
        } else {
            monitorInfos = monitorService.findAll();
        }
        List<DtoMonitor> dtoMonitors = new ArrayList<>();
        monitorInfos.forEach(monitorInfo -> {
            DtoMonitor dtoMonitor = new DtoMonitor();
            BeanUtils.copyProperties(monitorInfo, dtoMonitor);
            if (StringUtils.isNotBlank(monitorInfo.getDingdingHooks())) {
                List<String> dingdingHooks = new ArrayList<>();
                Collections.addAll(dingdingHooks, monitorInfo.getDingdingHooks().split(","));
                dtoMonitor.setDingdingHooks(dingdingHooks);
            }
            dtoMonitors.add(dtoMonitor);
        });
        return dtoMonitors;
    }

    @RequestMapping(value = "/save.api", method = RequestMethod.POST)
    public Msg saveOrUpdate(@RequestBody DtoMonitor req) throws SchedulerException {
        String msg = req.validate();
        if (msg != null) {
            return failed(msg);
        }
        LoginUser user = getCurrentUser();
        if (!user.isRoot()) {
            req.setUid(user.getId());
        }
        Date now = new Date();
        if (req.getId() == null) {
            req.setCreateTime(now);
        }
        req.setUpdateTime(now);
        Monitor monitor = new Monitor();
        BeanUtils.copyProperties(req, monitor);
        if (req.getDingdingHooks() != null && !req.getDingdingHooks().isEmpty()) {
            monitor.setDingdingHooks(StringUtils.join(req.getDingdingHooks(), ","));
        }
        monitor = monitorService.save(monitor);
        if (SchedulerUtils.checkExists(monitor.getId(), Constant.JobGroup.MONITOR)) {
            SchedulerUtils.deleteJob(monitor.getId(), Constant.JobGroup.MONITOR);
        }
        if (monitor.getStatus() == Constant.STATUS_ON) {
            AbstractMonitorRunner.build(monitor);
        }
        return success(monitor);
    }

    @RequestMapping(value = "/delete.api", method = RequestMethod.POST)
    public Msg delete(@RequestParam String id) throws SchedulerException {
        SchedulerUtils.deleteJob(id, Constant.JobGroup.MONITOR);
        monitorService.deleteById(id);
        return success();
    }
}
