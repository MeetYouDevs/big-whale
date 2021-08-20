package com.meiyou.bigwhale.controller;

import com.meiyou.bigwhale.common.Constant;
import com.meiyou.bigwhale.common.pojo.Msg;
import com.meiyou.bigwhale.dto.DtoMonitor;
import com.meiyou.bigwhale.dto.DtoScript;
import com.meiyou.bigwhale.entity.*;
import com.meiyou.bigwhale.scheduler.monitor.StreamJobMonitor;
import com.meiyou.bigwhale.scheduler.job.ScriptJob;
import com.meiyou.bigwhale.service.MonitorService;
import com.meiyou.bigwhale.security.LoginUser;
import com.meiyou.bigwhale.service.ScriptHistoryService;
import com.meiyou.bigwhale.service.ScriptService;
import com.meiyou.bigwhale.util.SchedulerUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/stream")
public class StreamController extends BaseController {

    @Autowired
    private ScriptService scriptService;
    @Autowired
    private MonitorService monitorService;
    @Autowired
    private ScriptHistoryService scriptHistoryService;

    @RequestMapping(value = "/page.api", method = RequestMethod.POST)
    public Msg page(@RequestBody DtoScript req) {
        LoginUser currentUser = getCurrentUser();
        if (!currentUser.isRoot()) {
            req.setCreateBy(currentUser.getId());
        }
        Page<DtoScript> dtoScriptPage = scriptService.fuzzyPage(req).map((script) -> {
            DtoScript dtoScript = new DtoScript();
            BeanUtils.copyProperties(script, dtoScript);
            Monitor monitor = monitorService.findById(script.getMonitorId());
            DtoMonitor dtoMonitor = new DtoMonitor();
            BeanUtils.copyProperties(monitor, dtoMonitor);
            if (StringUtils.isNotBlank(monitor.getWeek())) {
                List<String> weeks = new ArrayList<>();
                Collections.addAll(weeks, monitor.getWeek().split(","));
                dtoMonitor.setWeek(weeks);
            }
            if (StringUtils.isNotBlank(monitor.getDingdingHooks())) {
                List<String> dingdingHooks = new ArrayList<>();
                Collections.addAll(dingdingHooks, monitor.getDingdingHooks().split(","));
                dtoMonitor.setDingdingHooks(dingdingHooks);
            }
            dtoScript.setMonitor(dtoMonitor);
            return dtoScript;
        });
        return success(dtoScriptPage);
    }

    @RequestMapping(value = "/all.api", method = RequestMethod.GET)
    public Msg all() {
        LoginUser currentUser = getCurrentUser();
        List<Script> scripts;
        if (!currentUser.isRoot()) {
            scripts = scriptService.findByQuery("type=" + Constant.ScriptType.SPARK_STREAM + "," + Constant.ScriptType.FLINK_STREAM +
                    ";createBy=" + currentUser.getId());
        } else {
            scripts = scriptService.findByQuery("type=" + Constant.ScriptType.SPARK_STREAM + "," + Constant.ScriptType.FLINK_STREAM);
        }
        List<DtoScript> dtoScripts = scripts.stream().map((script) -> {
            DtoScript dtoScript = new DtoScript();
            BeanUtils.copyProperties(script, dtoScript);
            return dtoScript;
        }).collect(Collectors.toList());
        return success(dtoScripts);
    }

    @RequestMapping(value = "/one.api", method = RequestMethod.GET)
    public Msg one(@RequestParam Integer id) {
        Script script = scriptService.findById(id);
        if (script == null) {
            return failed();
        }
        DtoScript dtoScript = new DtoScript();
        BeanUtils.copyProperties(script, dtoScript);
        Monitor monitor = monitorService.findById(script.getMonitorId());
        DtoMonitor dtoMonitor = new DtoMonitor();
        BeanUtils.copyProperties(monitor, dtoMonitor);
        if (StringUtils.isNotBlank(monitor.getWeek())) {
            List<String> weeks = new ArrayList<>();
            Collections.addAll(weeks, monitor.getWeek().split(","));
            dtoMonitor.setWeek(weeks);
        }
        if (StringUtils.isNotBlank(monitor.getDingdingHooks())) {
            List<String> dingdingHooks = new ArrayList<>();
            Collections.addAll(dingdingHooks, monitor.getDingdingHooks().split(","));
            dtoMonitor.setDingdingHooks(dingdingHooks);
        }
        dtoScript.setMonitor(dtoMonitor);
        return success(dtoScript);
    }

    @RequestMapping(value = "/save.api", method = RequestMethod.POST)
    public Msg save(@RequestBody DtoScript req) {
        String msg = req.validate();
        if (msg != null) {
            return failed(msg);
        }
        LoginUser currentUser = getCurrentUser();
        Date now = new Date();
        if (req.getId() == null) {
            req.setCreateTime(now);
            req.setCreateBy(currentUser.getId());
        }
        msg = scriptService.validate(req);
        if (msg != null) {
            return failed("脚本【" + req.getName() + "】" + msg);
        }
        if (req.getId() == null) {
            Script dbScript = scriptService.findOneByQuery("name=" + req.getName());
            if (dbScript != null) {
                return failed( "任务已存在");
            }
        } else {
            Script dbScript = scriptService.findOneByQuery("name=" + req.getName() + ";id!=" + req.getId());
            if (dbScript != null) {
                return failed( "任务已存在");
            }
        }
        req.setUpdateTime(now);
        req.setUpdateBy(currentUser.getId());
        Script script = new Script();
        BeanUtils.copyProperties(req, script);
        script.setMonitorEnabled(req.getMonitor().getEnabled());
        Monitor monitor = new Monitor();
        BeanUtils.copyProperties(req.getMonitor(), monitor);
        if (req.getMonitor().getWeek() != null && !req.getMonitor().getWeek().isEmpty()) {
            monitor.setWeek(StringUtils.join(req.getMonitor().getWeek(), ","));
        }
        if (req.getMonitor().getDingdingHooks() != null && !req.getMonitor().getDingdingHooks().isEmpty()) {
            monitor.setDingdingHooks(StringUtils.join(req.getMonitor().getDingdingHooks(), ","));
        }
        if (monitor.getId() != null) {
            SchedulerUtils.interrupt(monitor.getId(), Constant.JobGroup.MONITOR);
            SchedulerUtils.deleteJob(monitor.getId(), Constant.JobGroup.MONITOR);
        }
        script = scriptService.update(script, monitor);
        monitor = monitorService.findById(script.getMonitorId());
        if (monitor.getEnabled()) {
            StreamJobMonitor.build(monitor);
        }
        return success();
    }

    @RequestMapping(value = "/delete.api", method = RequestMethod.POST)
    public Msg delete(@RequestBody DtoScript req) {
        Script script = scriptService.findById(req.getId());
        if (script == null) {
            return failed();
        }
        Monitor monitor = monitorService.findById(script.getMonitorId());
        SchedulerUtils.interrupt(monitor.getId(), Constant.JobGroup.MONITOR);
        SchedulerUtils.deleteJob(monitor.getId(), Constant.JobGroup.MONITOR);
        scriptService.delete(script);
        return success();
    }

    @RequestMapping(value = "/run.api", method = RequestMethod.POST)
    public Msg run(@RequestBody DtoScript req) {
        Script script = scriptService.findById(req.getId());
        if (script == null) {
            return failed();
        }
        ScriptHistory scriptHistory = scriptHistoryService.findScriptLatest(script.getId());
        if (scriptHistory != null && scriptHistory.isRunning()) {
            return failed("任务运行中，请勿重复执行");
        }
        Monitor monitor = monitorService.findById(script.getMonitorId());
        scriptHistory = scriptService.generateHistory(script, monitor);
        if (Constant.JobState.SUBMIT_WAIT.equals(scriptHistory.getState())) {
            scriptHistory.updateState(Constant.JobState.SUBMITTING);
            scriptHistory = scriptHistoryService.save(scriptHistory);
            ScriptJob.build(scriptHistory);
        }
        return success(scriptHistory);
    }

}