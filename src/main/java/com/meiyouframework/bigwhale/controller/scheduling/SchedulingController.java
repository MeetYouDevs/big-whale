package com.meiyouframework.bigwhale.controller.scheduling;

import com.meiyouframework.bigwhale.common.Constant;
import com.meiyouframework.bigwhale.common.pojo.Msg;
import com.meiyouframework.bigwhale.data.domain.PageRequest;
import com.meiyouframework.bigwhale.dto.DtoScheduling;
import com.meiyouframework.bigwhale.entity.CmdRecord;
import com.meiyouframework.bigwhale.entity.Scheduling;
import com.meiyouframework.bigwhale.entity.Script;
import com.meiyouframework.bigwhale.service.CmdRecordService;
import com.meiyouframework.bigwhale.service.SchedulingService;
import com.meiyouframework.bigwhale.controller.BaseController;
import com.meiyouframework.bigwhale.security.LoginUser;
import com.meiyouframework.bigwhale.service.ScriptService;
import com.meiyouframework.bigwhale.task.streaming.AbstractMonitorRunner;
import com.meiyouframework.bigwhale.task.batch.DagTask;
import com.meiyouframework.bigwhale.util.SchedulerUtils;
import org.apache.commons.lang.StringUtils;
import org.quartz.SchedulerException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/scheduling")
public class SchedulingController extends BaseController {

    private DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    private Map<Integer, String> scriptIconClass = new HashMap<>();

    @Autowired
    private SchedulingService schedulingService;
    @Autowired
    private ScriptService scriptService;
    @Autowired
    private CmdRecordService cmdRecordService;

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
                SchedulerUtils.deleteJob(dbScheduling.getId(), Constant.JobGroup.BATCH);
            } else {
                SchedulerUtils.deleteJob(dbScheduling.getId(), Constant.JobGroup.STREAMING);
            }
        }
        req.setUpdateTime(now);
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
                DagTask.build(scheduling);
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
                SchedulerUtils.deleteJob(scheduling.getId(), Constant.JobGroup.BATCH);
            } else {
                SchedulerUtils.deleteJob(scheduling.getId(), Constant.JobGroup.STREAMING);
            }
            schedulingService.deleteById(id);
        }
        return success();
    }

    @SuppressWarnings("unchecked")
    private void generateNodeTree(Scheduling scheduling, Map<String, Object> node, String currentNodeId) {
        Map<String, Scheduling.NodeData> nodeIdToData = scheduling.analyzeNextNode(currentNodeId);
        for (Map.Entry<String, Scheduling.NodeData> entry : nodeIdToData.entrySet()) {
            String nodeId = entry.getKey();
            String scriptId = entry.getValue().scriptId;
            Script script = scriptService.findById(scriptId);
            if (currentNodeId == null) {
                node.put("text", script.getName() + StringUtils.join(getCmdRecordStatus(scheduling, nodeId), ""));
                node.put("data", nodeId);
                node.put("icon", "iconfont " + scriptIconClass.get(script.getType()));
                generateNodeTree(scheduling, node, nodeId);
            } else {
                Map<String, Object> childNode = new HashMap<>();
                childNode.put("text", script.getName() + StringUtils.join(getCmdRecordStatus(scheduling, nodeId), ""));
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

    private List<String> getCmdRecordStatus(Scheduling scheduling, String nodeId) {
        List<CmdRecord> cmdRecords;
        if (scheduling.getType() == Constant.SCHEDULING_TYPE_BATCH) {
            if (scheduling.getLastExecuteTime() != null) {
                cmdRecords = cmdRecordService.findByQuery(
                        ";schedulingId=" + scheduling.getId() +
                                ";schedulingInstanceId=" + dateFormat.format(scheduling.getLastExecuteTime()) +
                                ";schedulingNodeId=" + nodeId,
                        Sort.by(Sort.Direction.ASC, "createTime"));
            } else {
                cmdRecords = new ArrayList<>();
            }

        } else {
            CmdRecord cmdRecord = cmdRecordService.findOneByQuery(
                    ";scriptId=" + scheduling.getScriptIds(),
                    Sort.by(Sort.Direction.DESC, "createTime"));
            if (cmdRecord != null) {
                cmdRecords = Collections.singletonList(cmdRecord);
            } else {
                cmdRecords = new ArrayList<>();
            }
        }
        return cmdRecords.stream().map(cmdRecord -> {
            if (cmdRecord.getStatus() == Constant.EXEC_STATUS_UNSTART) {
                return "<div class=\"cube label-info\"></div>";
            }
            if (cmdRecord.getStatus() == Constant.EXEC_STATUS_DOING)  {
                return "<div class=\"cube label-warning\"></div>";
            }
            if (cmdRecord.getStatus() == Constant.EXEC_STATUS_FINISH) {
                if (cmdRecord.getJobFinalStatus() == null) {
                    return "<div class=\"cube label-success\"></div>";
                } else {
                    if ("UNDEFINED".equals(cmdRecord.getJobFinalStatus())) {
                        return "<div class=\"cube label-warning\"></div>";
                    }
                    if ("SUCCEEDED".equals(cmdRecord.getJobFinalStatus())) {
                        return "<div class=\"cube label-success\"></div>";
                    }
                    return "<div class=\"cube label-danger\"></div>";
                }
            }
            return "<div class=\"cube label-danger\"></div>";
        }).collect(Collectors.toList());
    }
}
