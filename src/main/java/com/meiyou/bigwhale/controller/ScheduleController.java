package com.meiyou.bigwhale.controller;

import com.alibaba.fastjson.JSONObject;
import com.meiyou.bigwhale.common.Constant;
import com.meiyou.bigwhale.common.pojo.Msg;
import com.meiyou.bigwhale.data.domain.PageRequest;
import com.meiyou.bigwhale.dto.DtoScript;
import com.meiyou.bigwhale.dto.DtoSchedule;
import com.meiyou.bigwhale.entity.Script;
import com.meiyou.bigwhale.entity.Schedule;
import com.meiyou.bigwhale.entity.ScheduleSnapshot;
import com.meiyou.bigwhale.entity.ScriptHistory;
import com.meiyou.bigwhale.job.ScheduleJob;
import com.meiyou.bigwhale.job.ScriptHistoryShellRunnerJob;
import com.meiyou.bigwhale.service.ScheduleService;
import com.meiyou.bigwhale.security.LoginUser;
import com.meiyou.bigwhale.service.ScriptHistoryService;
import com.meiyou.bigwhale.service.ScriptService;
import com.meiyou.bigwhale.service.ScheduleSnapshotService;
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
    private ScheduleSnapshotService scheduleSnapshotService;
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
        ScheduleSnapshot scheduleSnapshot = scheduleSnapshotService.findByScheduleIdAndSnapshotTime(schedule.getId(), new Date());
        List<DtoScript> dtoScripts = new ArrayList<>();
        generateScripts(scheduleSnapshot, dtoScripts, null);
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
                String key = dtoScript.getClusterId() + "$" + dtoScript.getUser() + "$" + dtoScript.getQueue() + "$" + dtoScript.getApp();
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

    @RequestMapping(value = "/treeview.api", method = RequestMethod.GET)
    public Msg treeView(@RequestParam Integer id,
                        @RequestParam String instance) {
        Integer scheduleSnapshotId = scriptHistoryService.findOneByQuery("scheduleId=" + id + ";scheduleInstanceId=" + instance,
                new Sort(Sort.Direction.DESC, "id")).getScheduleSnapshotId();
        ScheduleSnapshot scheduleSnapshot = scheduleSnapshotService.findById(scheduleSnapshotId);
        Map<String, Object> nodeTree = new HashMap<>();
        generateNodeTree(scheduleSnapshot, nodeTree, null, instance);
        return success(Collections.singletonList(nodeTree));
    }

    @RequestMapping(value = "/run.api", method = RequestMethod.POST)
    public Msg run(@RequestBody DtoSchedule req) {
        Schedule schedule = scheduleService.findById(req.getId());
        if (schedule == null) {
            return failed();
        }
        Date now = new Date();
        ScheduleSnapshot scheduleSnapshot = scheduleSnapshotService.findByScheduleIdAndSnapshotTime(req.getId(), now);
        String instanceId = new SimpleDateFormat("yyyyMMddHHmmss").format(now);
        generateScriptHistories(scheduleSnapshot, null, instanceId, now, now, false);
        schedule.setRealFireTime(now);
        scheduleService.save(schedule);
        DtoSchedule dtoSchedule = new DtoSchedule();
        BeanUtils.copyProperties(schedule, dtoSchedule);
        Map<String, Object> result = new HashMap<>();
        result.put("instanceId", instanceId);
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
        ScheduleSnapshot scheduleSnapshot = scheduleSnapshotService.findByScheduleIdAndSnapshotTime(id, now);
        Date needFireTime = ScheduleJob.getNeedFireTime(scheduleSnapshot.generateCron(), startTime);
        // 获取第一个触发时间
        while (needFireTime.compareTo(startTime) < 0) {
            needFireTime = ScheduleJob.getNextFireTime(scheduleSnapshot.generateCron(), needFireTime);
        }
        if (needFireTime.compareTo(endTime) > 0) {
            return failed("时间范围有误");
        }
        do {
            generateScriptHistories(scheduleSnapshot, null, dateFormat.format(needFireTime), needFireTime, now, true);
            needFireTime = ScheduleJob.getNextFireTime(scheduleSnapshot.generateCron(), needFireTime);
        } while (needFireTime.compareTo(endTime) < 0);
        return success();
    }

    private void generateScripts(ScheduleSnapshot scheduleSnapshot, List<DtoScript> dtoScripts, String currentNodeId) {
        Map<String, ScheduleSnapshot.Topology.Node> nodeIdToData = scheduleSnapshot.analyzeNextNode(currentNodeId);
        for (Map.Entry<String,  ScheduleSnapshot.Topology.Node> entry : nodeIdToData.entrySet()) {
            String nodeId = entry.getKey();
            Script script = scriptService.findOneByQuery("scheduleId=" + scheduleSnapshot.getScheduleId() + ";scheduleTopNodeId=" + nodeId);
            DtoScript dtoScript = new DtoScript();
            BeanUtils.copyProperties(script, dtoScript);
            dtoScripts.add(dtoScript);
            generateScripts(scheduleSnapshot, dtoScripts, nodeId);
        }
    }

    @SuppressWarnings("unchecked")
    private void generateNodeTree(ScheduleSnapshot scheduleSnapshot, Map<String, Object> nodeTree, String currentNodeId, String instanceId) {
        Map<String, ScheduleSnapshot.Topology.Node> nodeIdToData = scheduleSnapshot.analyzeNextNode(currentNodeId);
        for (Map.Entry<String,  ScheduleSnapshot.Topology.Node> entry : nodeIdToData.entrySet()) {
            String nodeId = entry.getKey();
            Script script = scriptService.findOneByQuery("scheduleId=" + scheduleSnapshot.getScheduleId() + ";scheduleTopNodeId=" + nodeId);
            String stateTag = null;
            if (instanceId != null) {
                stateTag = StringUtils.join(getStateTag(scheduleSnapshot, nodeId, instanceId), "");
            }
            if (currentNodeId == null) {
                if (stateTag != null) {
                    nodeTree.put("text", script.getName() + stateTag);
                    nodeTree.put("rerunEnabled_", !stateTag.contains("label-default"));
                } else {
                    nodeTree.put("text", script.getName());
                }
                nodeTree.put("nodeId_", nodeId);
                nodeTree.put("snapshotId_", scheduleSnapshot.getId());
                nodeTree.put("icon", "iconfont " + scriptIconClass.get(script.getType()));
                nodeTree.put("state", Collections.singletonMap("expanded", true));
                generateNodeTree(scheduleSnapshot, nodeTree, nodeId, instanceId);
            } else {
                Map<String, Object> childNode = new HashMap<>();
                if (stateTag != null) {
                    childNode.put("text", script.getName() + stateTag);
                    childNode.put("rerunEnabled_", !stateTag.contains("label-default"));
                } else {
                    childNode.put("text", script.getName());
                }
                childNode.put("nodeId_", nodeId);
                childNode.put("snapshotId_", scheduleSnapshot.getId());
                childNode.put("icon", "iconfont " + scriptIconClass.get(script.getType()));
                childNode.put("state", Collections.singletonMap("expanded", true));
                List<Map<String, Object>> childNodes = (List<Map<String, Object>>)nodeTree.get("nodes");
                if (childNodes == null) {
                    childNodes = new ArrayList<>();
                    nodeTree.put("nodes", childNodes);
                }
                childNodes.add(childNode);
                generateNodeTree(scheduleSnapshot, childNode, nodeId, instanceId);
            }
        }
    }

    private void generateScriptHistories(ScheduleSnapshot scheduleSnapshot, String currentNodeId, String instanceId, Date instanceTime, Date now, boolean supplement) {
        Map<String, ScheduleSnapshot.Topology.Node> nodeIdToData = scheduleSnapshot.analyzeNextNode(currentNodeId);
        for (Map.Entry<String,  ScheduleSnapshot.Topology.Node> entry : nodeIdToData.entrySet()) {
            String nodeId = entry.getKey();
            Script script = scriptService.findOneByQuery("scheduleId=" + scheduleSnapshot.getScheduleId() + ";scheduleTopNodeId=" + nodeId);
            ScriptHistory scriptHistory = scriptService.generateHistory(script, scheduleSnapshot, instanceId, supplement ? 2 : 0);
            scriptHistory.updateState(Constant.JobState.WAITING_PARENT_);
            scriptHistory.updateState(Constant.JobState.INITED);
            scriptHistory = scriptHistoryService.save(scriptHistory);
            if (instanceTime.compareTo(now) <= 0) {
                ScriptHistoryShellRunnerJob.build(scriptHistory);
            } else {
                ScriptHistoryShellRunnerJob.build(scriptHistory, instanceTime);
            }
            generateScriptHistories(scheduleSnapshot, nodeId, instanceId, instanceTime, now, supplement);
        }
    }

    private List<String> getStateTag(ScheduleSnapshot scheduleSnapshot, String nodeId, String instanceId) {
        List<ScriptHistory> scriptHistories = scriptHistoryService.findByQuery(
                        "scheduleId=" + scheduleSnapshot.getScheduleId() +
                                ";scheduleTopNodeId=" + nodeId +
                                ";scheduleInstanceId=" + instanceId,
                        Sort.by(Sort.Direction.ASC, "id"));
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
