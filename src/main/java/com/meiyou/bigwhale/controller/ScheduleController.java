package com.meiyou.bigwhale.controller;

import com.alibaba.fastjson.JSONObject;
import com.meiyou.bigwhale.common.Constant;
import com.meiyou.bigwhale.common.pojo.Msg;
import com.meiyou.bigwhale.data.domain.PageRequest;
import com.meiyou.bigwhale.dto.DtoScript;
import com.meiyou.bigwhale.dto.DtoSchedule;
import com.meiyou.bigwhale.entity.Script;
import com.meiyou.bigwhale.entity.Schedule;
import com.meiyou.bigwhale.entity.ScriptHistory;
import com.meiyou.bigwhale.job.ScheduleJob;
import com.meiyou.bigwhale.job.ScriptHistoryShellRunnerJob;
import com.meiyou.bigwhale.service.ScheduleService;
import com.meiyou.bigwhale.security.LoginUser;
import com.meiyou.bigwhale.service.ScriptHistoryService;
import com.meiyou.bigwhale.service.ScriptService;
import com.meiyou.bigwhale.util.SchedulerUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/schedule")
public class ScheduleController extends BaseController {

    private Map<String, String> scriptIconClass = new HashMap<>();

    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private ScriptService scriptService;
    @Autowired
    private ScriptHistoryService scriptHistoryService;

    public ScheduleController() {
        scriptIconClass.put("shell", "icon-shell");
        scriptIconClass.put("python", "icon-python");
        scriptIconClass.put("sparkbatch", "icon-spark");
        scriptIconClass.put("flinkbatch", "icon-flink");
    }

    @RequestMapping(value = "/page.api", method = RequestMethod.POST)
    public Msg page(@RequestBody DtoSchedule req) {
        LoginUser currentUser = getCurrentUser();
        if (!currentUser.isRoot()) {
            req.setCreateBy(currentUser.getId());
        }
        List<String> tokens = new ArrayList<>();
        if (StringUtils.isNotBlank(req.getKeyword())) {
            tokens.add("keyword?" + req.getKeyword());
        }
        if (req.getEnabled() != null) {
            tokens.add("enabled=" + req.getEnabled());
        }
        if (req.getCreateBy() != null) {
            tokens.add("createBy=" + req.getCreateBy());
        }
        Page<DtoSchedule> dtoSchedulePage = scheduleService.pageByQuery(new PageRequest(req.pageNo - 1, req.pageSize, StringUtils.join(tokens, ";"))).map((item) -> {
            DtoSchedule dtoSchedule = new DtoSchedule();
            BeanUtils.copyProperties(item, dtoSchedule);
            if (StringUtils.isNotBlank(item.getWeek())) {
                List<String> weeks = new ArrayList<>();
                Collections.addAll(weeks, item.getWeek().split(","));
                dtoSchedule.setWeek(weeks);
            }
            if (StringUtils.isNotBlank(item.getDingdingHooks())) {
                List<String> dingdingHooks = new ArrayList<>();
                Collections.addAll(dingdingHooks, item.getDingdingHooks().split(","));
                dtoSchedule.setDingdingHooks(dingdingHooks);
            }
            return dtoSchedule;
        });
        return success(dtoSchedulePage);
    }

    @RequestMapping(value = "/all.api", method = RequestMethod.GET)
    public Msg all() {
        LoginUser currentUser = getCurrentUser();
        List<Schedule> schedules;
        if (!currentUser.isRoot()) {
            schedules = scheduleService.findByQuery("createBy=" + currentUser.getId());
        } else {
            schedules = scheduleService.findByQuery(null);
        }
        List<DtoSchedule> dtoSchedules = schedules.stream().map((schedule) -> {
            DtoSchedule dtoSchedule = new DtoSchedule();
            BeanUtils.copyProperties(schedule, dtoSchedule);
            return dtoSchedule;
        }).collect(Collectors.toList());
        return success(dtoSchedules);
    }

    @RequestMapping(value = "/one.api", method = RequestMethod.GET)
    public Msg one(@RequestParam Integer id) {
        Schedule schedule = scheduleService.findById(id);
        if (schedule == null) {
            return failed();
        }
        DtoSchedule dtoSchedule = new DtoSchedule();
        BeanUtils.copyProperties(schedule, dtoSchedule);
        if (StringUtils.isNotBlank(schedule.getWeek())) {
            List<String> weeks = new ArrayList<>();
            Collections.addAll(weeks, schedule.getWeek().split(","));
            dtoSchedule.setWeek(weeks);
        }
        if (StringUtils.isNotBlank(schedule.getDingdingHooks())) {
            List<String> dingdingHooks = new ArrayList<>();
            Collections.addAll(dingdingHooks, schedule.getDingdingHooks().split(","));
            dtoSchedule.setDingdingHooks(dingdingHooks);
        }
        List<DtoScript> dtoScripts = new ArrayList<>();
        generateScripts(schedule, null, dtoScripts);
        dtoSchedule.setScripts(dtoScripts);
        return success(dtoSchedule);
    }

    @RequestMapping(value = "/save.api", method = RequestMethod.POST)
    public Msg save(@RequestBody DtoSchedule req) {
        String msg = req.validate();
        if (msg != null) {
            return failed(msg);
        }
        LoginUser currentUser = getCurrentUser();
        Date now = new Date();
        for (DtoScript dtoScript : req.getScripts()) {
            if (dtoScript.getId() == null) {
                dtoScript.setCreateTime(now);
                dtoScript.setCreateBy(currentUser.getId());
            }
            dtoScript.setUpdateTime(now);
            dtoScript.setUpdateBy(currentUser.getId());
        }
        for (DtoScript dtoScript : req.getScripts()) {
            msg = scriptService.validate(dtoScript);
            if (msg != null) {
                return failed("脚本【" + dtoScript.getName() + "】" + msg);
            }
        }
        // 当前重复脚本检查
        Set<String> keys = new HashSet<>();
        for (DtoScript dtoScript : req.getScripts()) {
            if (dtoScript.isYarn()) {
                String key = dtoScript.getClusterId() + ";" + dtoScript.getUser() + ";" + dtoScript.getQueue() + ";" + dtoScript.getApp();
                if (keys.contains(key)) {
                    return failed("脚本【" + dtoScript.getName() + "】YARN应用重复");
                }
                keys.add(key);
            }
        }
        if (req.getId() == null) {
            Schedule dbSchedule = scheduleService.findOneByQuery("name=" + req.getName());
            if (dbSchedule != null) {
                return failed( "调度已存在");
            }
            req.setCreateTime(now);
            req.setCreateBy(currentUser.getId());
        } else {
            Schedule dbSchedule = scheduleService.findOneByQuery("name=" + req.getName() + ";id!=" + req.getId());
            if (dbSchedule != null) {
                return failed( "调度已存在");
            }
        }
        req.setUpdateTime(now);
        req.setUpdateBy(currentUser.getId());
        Schedule schedule = new Schedule();
        BeanUtils.copyProperties(req, schedule);
        if (req.getWeek() != null && !req.getWeek().isEmpty()) {
            schedule.setWeek(StringUtils.join(req.getWeek(), ","));
        }
        if (req.getDingdingHooks() != null && !req.getDingdingHooks().isEmpty()) {
            schedule.setDingdingHooks(StringUtils.join(req.getDingdingHooks(), ","));
        }
        StringBuilder keyword = new StringBuilder(req.getName() + "_" + req.getDescription());
        List<Script> scripts = new ArrayList<>();
        for (DtoScript dtoScript : req.getScripts()) {
            Script script = new Script();
            BeanUtils.copyProperties(dtoScript, script);
            scripts.add(script);
            keyword.append("_").append(script.getName()).append("_").append(script.getApp());
        }
        schedule.setKeyword(keyword.toString().replaceAll("_null", "_-"));
        scheduleService.update(schedule, scripts);
        return success();
    }

    @RequestMapping(value = "/delete.api", method = RequestMethod.POST)
    public Msg delete(@RequestBody DtoSchedule req) {
        Schedule schedule = scheduleService.findById(req.getId());
        if (schedule == null) {
            return failed();
        }
        SchedulerUtils.deleteJob(req.getId(), Constant.JobGroup.SCHEDULE);
        scheduleService.delete(schedule);
        return success();
    }

    @RequestMapping(value = "/instance.api", method = RequestMethod.POST)
    public Msg instance(@RequestBody DtoSchedule req) {
        Schedule schedule = scheduleService.findById(req.getId());
        if (schedule == null) {
            return failed();
        }
        DtoSchedule dtoSchedule = new DtoSchedule();
        BeanUtils.copyProperties(schedule, dtoSchedule);
        Page<String> instance = scheduleService.instancePage(req);
        return success(instance);
    }

    @RequestMapping(value = "/run.api", method = RequestMethod.POST)
    public Msg run(@RequestBody DtoSchedule req) {
        Schedule schedule = scheduleService.findById(req.getId());
        if (schedule == null) {
            return failed();
        }
        Date now = new Date();
        String scheduleInstanceId = new SimpleDateFormat("yyyyMMddHHmmss").format(now);
        generateScriptHistories(schedule, scheduleInstanceId, null, now, now, false);
        schedule.setRealFireTime(now);
        scheduleService.save(schedule);
        DtoSchedule dtoSchedule = new DtoSchedule();
        BeanUtils.copyProperties(schedule, dtoSchedule);
        Map<String, Object> result = new HashMap<>();
        result.put("instanceId", scheduleInstanceId);
        result.put("obj", dtoSchedule);
        return success(result);
    }

    @RequestMapping(value = "/supplement.api", method = RequestMethod.POST)
    public Msg supplement(@RequestBody JSONObject params) throws ParseException {
        Integer id = params.getInteger("id");
        Schedule schedule = scheduleService.findById(id);
        if (schedule == null) {
            return failed();
        }
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        DateFormat dateFormat1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date startTime = dateFormat1.parse(params.getString("start"));
        Date endTime = dateFormat1.parse(params.getString("end"));
        Date now = new Date();
        Date needFireTime = ScheduleJob.getNeedFireTime(schedule.generateCron(), startTime);
        // 获取第一个触发时间
        while (needFireTime.compareTo(startTime) < 0) {
            needFireTime = ScheduleJob.getNextFireTime(schedule.generateCron(), needFireTime);
        }
        if (needFireTime.compareTo(endTime) > 0) {
            return failed("时间范围有误");
        }
        do {
            generateScriptHistories(schedule, dateFormat.format(needFireTime), null, needFireTime, now, true);
            needFireTime = ScheduleJob.getNextFireTime(schedule.generateCron(), needFireTime);
        } while (needFireTime.compareTo(endTime) < 0);
        return success();
    }

    @RequestMapping(value = "/treeview.api", method = RequestMethod.GET)
    public Msg treeView(@RequestParam Integer id,
                        @RequestParam String instance) {
        Schedule schedule = scheduleService.findById(id);
        if (schedule == null) {
            return failed();
        }
        Map<String, Object> nodeTree = new HashMap<>();
        List<ScriptHistory> scriptHistories = scriptHistoryService.findByQuery(
                "scheduleId=" + schedule.getId() +
                        ";scheduleInstanceId=" + instance,
                Sort.by(Sort.Direction.ASC, "id"));
        generateNodeTree(null, scriptHistories, nodeTree);
        return success(Collections.singletonList(nodeTree));
    }

    private void generateScripts(Schedule schedule, String previousScheduleTopNodeId, List<DtoScript> dtoScripts) {
        Map<String, Schedule.Topology.Node> nodeIdToData = schedule.analyzeNextNode(previousScheduleTopNodeId);
        for (String nodeId : nodeIdToData.keySet()) {
            Script script = scriptService.findOneByQuery("scheduleId=" + schedule.getId() + ";scheduleTopNodeId=" + nodeId);
            DtoScript dtoScript = new DtoScript();
            BeanUtils.copyProperties(script, dtoScript);
            dtoScripts.add(dtoScript);
            generateScripts(schedule, nodeId, dtoScripts);
        }
    }

    private void generateScriptHistories(Schedule schedule, String scheduleInstanceId, String previousScheduleTopNodeId, Date instanceTime, Date now, boolean supplement) {
        Map<String, Schedule.Topology.Node> nodeIdToData = schedule.analyzeNextNode(previousScheduleTopNodeId);
        for (String nodeId : nodeIdToData.keySet()) {
            Script script = scriptService.findOneByQuery("scheduleId=" + schedule.getId() + ";scheduleTopNodeId=" + nodeId);
            ScriptHistory scriptHistory = scriptService.generateHistory(script, schedule, scheduleInstanceId, previousScheduleTopNodeId, supplement ? 2 : 0);
            scriptHistory.updateState(Constant.JobState.WAITING_PARENT_);
            if (supplement) {
                scriptHistory.updateState(Constant.JobState.INITED);
            }
            scriptHistory = scriptHistoryService.save(scriptHistory);
            if (supplement) {
                if (instanceTime.compareTo(now) <= 0) {
                    ScriptHistoryShellRunnerJob.build(scriptHistory);
                } else {
                    ScriptHistoryShellRunnerJob.build(scriptHistory, instanceTime);
                }
            }
            generateScriptHistories(schedule, scheduleInstanceId, nodeId, instanceTime, now, supplement);
        }
    }

    private void generateNodeTree(String previousScheduleTopNodeId, List<ScriptHistory> scriptHistories, Map<String, Object> nodeTree) {
        Map<String, List<ScriptHistory>> topScriptHistories = new HashMap<>();
        for (ScriptHistory scriptHistory : scriptHistories) {
            if (Objects.equals(scriptHistory.getPreviousScheduleTopNodeId(), previousScheduleTopNodeId)) {
                List<ScriptHistory> topScriptHistory = topScriptHistories.computeIfAbsent(scriptHistory.getScheduleTopNodeId(), k -> new ArrayList<>());
                topScriptHistory.add(scriptHistory);
            }
        }
        for (List<ScriptHistory> histories : topScriptHistories.values()) {
            String stateTag = StringUtils.join(getStateTag(histories), "");
            ScriptHistory history = histories.get(0);
            if (previousScheduleTopNodeId == null) {
                if (stateTag != null) {
                    nodeTree.put("text", history.getScriptName() + stateTag);
                    nodeTree.put("rerunEnabled_", !stateTag.contains("label-default"));
                } else {
                    nodeTree.put("text", history.getScriptName());
                }
                nodeTree.put("nodeId_", history.getScheduleTopNodeId());
                nodeTree.put("scheduleId_", history.getScheduleId());
                nodeTree.put("icon", "iconfont " + scriptIconClass.get(history.getScriptType()));
                nodeTree.put("state", Collections.singletonMap("expanded", true));
                generateNodeTree(history.getScheduleTopNodeId(), scriptHistories, nodeTree);
            } else {
                Map<String, Object> childNode = new HashMap<>();
                if (stateTag != null) {
                    childNode.put("text", history.getScriptName() + stateTag);
                    childNode.put("rerunEnabled_", !stateTag.contains("label-default"));
                } else {
                    childNode.put("text", history.getScriptName());
                }
                childNode.put("nodeId_", history.getScheduleTopNodeId());
                childNode.put("scheduleId_", history.getScheduleId());
                childNode.put("icon", "iconfont " + scriptIconClass.get(history.getScriptType()));
                childNode.put("state", Collections.singletonMap("expanded", true));
                List<Map<String, Object>> childNodes = (List<Map<String, Object>>)nodeTree.get("nodes");
                if (childNodes == null) {
                    childNodes = new ArrayList<>();
                    nodeTree.put("nodes", childNodes);
                }
                childNodes.add(childNode);
                generateNodeTree(history.getScheduleTopNodeId(), scriptHistories, childNode);
            }
        }
    }

    private List<String> getStateTag(List<ScriptHistory> scriptHistories) {
        return scriptHistories.stream().map(scriptHistory -> {
            if (Constant.JobState.UN_CONFIRMED_.equals(scriptHistory.getState())) {
                return "<span class=\"cube label-default\"></span>";
            }
            if (Constant.JobState.WAITING_PARENT_.equals(scriptHistory.getState())) {
                return "<span class=\"cube label-warning\"></span>";
            }
            if (scriptHistory.isRunning()) {
                return "<span class=\"cube label-info\"></span>";
            }
            if (Constant.JobState.SUCCEEDED.equals(scriptHistory.getState())) {
                return "<span class=\"cube label-success\"></span>";
            }
            return "<span class=\"cube label-danger\"></span>";
        }).collect(Collectors.toList());
    }

}
