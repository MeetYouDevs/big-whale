package com.meiyouframework.bigwhale.controller.scheduling;

import com.meiyouframework.bigwhale.common.Constant;
import com.meiyouframework.bigwhale.common.pojo.Msg;
import com.meiyouframework.bigwhale.data.domain.PageRequest;
import com.meiyouframework.bigwhale.dto.DtoScheduling;
import com.meiyouframework.bigwhale.entity.Scheduling;
import com.meiyouframework.bigwhale.service.SchedulingService;
import com.meiyouframework.bigwhale.controller.BaseController;
import com.meiyouframework.bigwhale.security.LoginUser;
import com.meiyouframework.bigwhale.task.timed.TimedTask;
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
@RequestMapping("/scheduling")
public class SchedulingController extends BaseController {

    @Autowired
    private SchedulingService schedulingService;

    @RequestMapping(value = "/getpage.api", method = RequestMethod.POST)
    public Page<DtoScheduling> getPage(@RequestBody DtoScheduling req) {
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
        Page<Scheduling> pages = schedulingService.pageByQuery(new PageRequest(req.pageNo - 1, req.pageSize, StringUtils.join(tokens, ";")));
        return pages.map((scheduling) -> {
            DtoScheduling dtoScheduling = new DtoScheduling();
            BeanUtils.copyProperties(scheduling, dtoScheduling);
            if (StringUtils.isNotBlank(scheduling.getSubScriptIds())) {
                List<String> subScriptIds = new ArrayList<>();
                Collections.addAll(subScriptIds, scheduling.getSubScriptIds().split(","));
                dtoScheduling.setSubScriptIds(subScriptIds);
            }
            if (StringUtils.isNotBlank(scheduling.getWeek())) {
                List<String> weeks = new ArrayList<>();
                Collections.addAll(weeks, scheduling.getWeek().split(","));
                dtoScheduling.setWeek(weeks);
            }
            if (StringUtils.isNotBlank(scheduling.getDingdingHooks())) {
                List<String> dingdingHooks = new ArrayList<>();
                Collections.addAll(dingdingHooks, scheduling.getDingdingHooks().split(","));
                dtoScheduling.setDingdingHooks(dingdingHooks);
            }
            return dtoScheduling;
        });
    }

    @RequestMapping(value = "/getall.api", method = RequestMethod.GET)
    public Iterable<DtoScheduling> getAll() {
        LoginUser user = getCurrentUser();
        Iterable<Scheduling> schedulings;
        if (!user.isRoot()) {
            schedulings = schedulingService.findByQuery("uid=" + user.getId());
        } else {
            schedulings = schedulingService.findAll();
        }
        List<DtoScheduling> dtoSchedulings = new ArrayList<>();
        schedulings.forEach(scheduling -> {
            DtoScheduling dtoScheduling = new DtoScheduling();
            BeanUtils.copyProperties(scheduling, dtoScheduling);
            if (StringUtils.isNotBlank(scheduling.getSubScriptIds())) {
                List<String> subScriptIds = new ArrayList<>();
                Collections.addAll(subScriptIds, scheduling.getSubScriptIds().split(","));
                dtoScheduling.setSubScriptIds(subScriptIds);
            }
            if (StringUtils.isNotBlank(scheduling.getWeek())) {
                List<String> weeks = new ArrayList<>();
                Collections.addAll(weeks, scheduling.getWeek().split(","));
                dtoScheduling.setWeek(weeks);
            }
            if (StringUtils.isNotBlank(scheduling.getDingdingHooks())) {
                List<String> dingdingHooks = new ArrayList<>();
                Collections.addAll(dingdingHooks, scheduling.getDingdingHooks().split(","));
                dtoScheduling.setDingdingHooks(dingdingHooks);
            }
            dtoSchedulings.add(dtoScheduling);
        });
        return dtoSchedulings;
    }

    @RequestMapping(value = "/save.api", method = RequestMethod.POST)
    public Msg saveOrUpdate(@RequestBody DtoScheduling req) throws SchedulerException {
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
        req.setLastExecuteTime(null);
        req.setUpdateTime(now);
        Scheduling scheduling = new Scheduling();
        BeanUtils.copyProperties(req, scheduling);
        if (req.getSubScriptIds() != null && !req.getSubScriptIds().isEmpty()) {
            scheduling.setSubScriptIds(StringUtils.join(req.getSubScriptIds(), ","));
        }
        if (req.getWeek() != null && !req.getWeek().isEmpty()) {
            scheduling.setWeek(StringUtils.join(req.getWeek(), ","));
        }
        if (req.getDingdingHooks() != null && !req.getDingdingHooks().isEmpty()) {
            scheduling.setDingdingHooks(StringUtils.join(req.getDingdingHooks(), ","));
        }
        scheduling = schedulingService.save(scheduling);
        if (SchedulerUtils.checkExists(scheduling.getId(), Constant.JobGroup.TIMED)) {
            SchedulerUtils.deleteJob(scheduling.getId(), Constant.JobGroup.TIMED);
        }
        if (scheduling.getStatus() == Constant.STATUS_ON) {
            TimedTask.build(scheduling);
        }
        return success(scheduling);

    }

    @RequestMapping(value = "/delete.api", method = RequestMethod.POST)
    public Msg delete(@RequestParam String id) throws SchedulerException {
        SchedulerUtils.deleteJob(id, Constant.JobGroup.TIMED);
        schedulingService.deleteById(id);
        return success();
    }
}
