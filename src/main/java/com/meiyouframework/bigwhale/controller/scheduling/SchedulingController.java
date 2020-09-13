package com.meiyouframework.bigwhale.controller.scheduling;

import com.meiyouframework.bigwhale.common.Constant;
import com.meiyouframework.bigwhale.common.pojo.Msg;
import com.meiyouframework.bigwhale.data.domain.PageRequest;
import com.meiyouframework.bigwhale.dto.DtoScheduling;
import com.meiyouframework.bigwhale.entity.Scheduling;
import com.meiyouframework.bigwhale.entity.Script;
import com.meiyouframework.bigwhale.service.SchedulingService;
import com.meiyouframework.bigwhale.controller.BaseController;
import com.meiyouframework.bigwhale.security.LoginUser;
import com.meiyouframework.bigwhale.service.ScriptService;
import com.meiyouframework.bigwhale.task.monitor.AbstractMonitorRunner;
import com.meiyouframework.bigwhale.task.timed.TimedTask;
import com.meiyouframework.bigwhale.util.SchedulerUtils;
import org.apache.commons.lang.StringUtils;
import org.quartz.SchedulerException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/scheduling")
public class SchedulingController extends BaseController {

    private Map<Integer, String> scriptIconClass = new HashMap<>();

    @Autowired
    private SchedulingService schedulingService;
    @Autowired
    private ScriptService scriptService;

    public SchedulingController() {
        scriptIconClass.put(0, "icon-shell");
        scriptIconClass.put(1, "icon-spark");
        scriptIconClass.put(2, "icon-spark");
        scriptIconClass.put(3, "icon-flink");
        scriptIconClass.put(4, "icon-flink");
    }

    @RequestMapping(value = "/getpage.api", method = RequestMethod.POST)
    public Msg getPage(@RequestBody DtoScheduling req) {
        LoginUser user = getCurrentUser();
        if (!user.isRoot()) {
            req.setUid(user.getId());
        }
        List<String> tokens = new ArrayList<>();
        if (StringUtils.isNotBlank(req.getId())) {
            tokens.add("id=" + req.getId());
        }
        if (StringUtils.isNotBlank(req.getUid())) {
            tokens.add("uid=" + req.getUid());
        }
        if (req.getType() != null) {
            tokens.add("type=" + req.getType());
        }
        if (StringUtils.isNotBlank(req.getScriptId())) {
            tokens.add("scriptIds?" + req.getScriptId());
        }
        Page<Scheduling> schedulingPage = schedulingService.pageByQuery(new PageRequest(req.pageNo - 1, req.pageSize, StringUtils.join(tokens, ";")));
        Page<DtoScheduling> dtoSchedulingPage = schedulingPage.map((scheduling) -> {
            DtoScheduling dtoScheduling = new DtoScheduling();
            BeanUtils.copyProperties(scheduling, dtoScheduling);
            dtoScheduling.setScriptIds(Arrays.asList(scheduling.getScriptIds().split(",")));
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
            Map<String, Object> nodeTree = new HashMap<>();
            generateNodeTree(scheduling, nodeTree, null);
            dtoScheduling.setNodeTree(Collections.singletonList(nodeTree));
            return dtoScheduling;
        });
        return success(dtoSchedulingPage);
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
        } else {
            Scheduling dbScheduling = schedulingService.findById(req.getId());
            if (dbScheduling == null) {
                return failed();
            }
            if (dbScheduling.getType() == Constant.SCHEDULING_TYPE_BATCH) {
                SchedulerUtils.deleteJob(dbScheduling.getId(), Constant.JobGroup.TIMED);
            } else {
                SchedulerUtils.deleteJob(dbScheduling.getId(), Constant.JobGroup.MONITOR);
            }
        }
        req.setUpdateTime(now);
        req.setLastExecuteTime(null);
        Scheduling scheduling = new Scheduling();
        BeanUtils.copyProperties(req, scheduling);
        scheduling.setScriptIds(StringUtils.join(req.getScriptIds(), ","));
        if (req.getWeek() != null && !req.getWeek().isEmpty()) {
            scheduling.setWeek(StringUtils.join(req.getWeek(), ","));
        }
        if (req.getDingdingHooks() != null && !req.getDingdingHooks().isEmpty()) {
            scheduling.setDingdingHooks(StringUtils.join(req.getDingdingHooks(), ","));
        }
        scheduling = schedulingService.save(scheduling);
        if (scheduling.getEnabled()) {
            if (scheduling.getType() == Constant.SCHEDULING_TYPE_BATCH) {
                TimedTask.build(scheduling);
            } else {
                AbstractMonitorRunner.build(scheduling);
            }
        }
        if (req.getId() == null) {
            req.setId(scheduling.getId());
        }
        return success(req);
    }

    @RequestMapping(value = "/delete.api", method = RequestMethod.POST)
    public Msg delete(@RequestParam String id) throws SchedulerException {
        Scheduling scheduling = schedulingService.findById(id);
        if (scheduling != null) {
            if (scheduling.getType() == Constant.SCHEDULING_TYPE_BATCH) {
                SchedulerUtils.deleteJob(scheduling.getId(), Constant.JobGroup.TIMED);
            } else {
                SchedulerUtils.deleteJob(scheduling.getId(), Constant.JobGroup.MONITOR);
            }
            schedulingService.deleteById(id);
        }
        return success();
    }

    @SuppressWarnings("unchecked")
    private void generateNodeTree(Scheduling scheduling, Map<String, Object> node, String currentNodeId) {
        Map<String, String> nodeIdToScriptId = scheduling.analyzeNextNode(currentNodeId);
        for (Map.Entry<String, String> entry : nodeIdToScriptId.entrySet()) {
            String nodeId = entry.getKey();
            String scriptId = entry.getValue();
            Script script = scriptService.findById(scriptId);
            if (currentNodeId == null) {
                node.put("text", script.getName());
                node.put("data", nodeId);
                node.put("icon", "iconfont " + scriptIconClass.get(script.getType()));
                generateNodeTree(scheduling, node, nodeId);
            } else {
                Map<String, Object> childNode = new HashMap<>();
                childNode.put("text", script.getName());
                childNode.put("data", nodeId);
                childNode.put("icon", "iconfont " + scriptIconClass.get(script.getType()));
                List<Map<String, Object>> childNodes = (List<Map<String, Object>>)node.get("nodes");
                if (childNodes == null) {
                    childNodes = new ArrayList<>();
                    node.put("nodes", childNodes);
                }
                childNodes.add(childNode);
                generateNodeTree(scheduling, childNode, nodeId);
            }
        }
    }
}
