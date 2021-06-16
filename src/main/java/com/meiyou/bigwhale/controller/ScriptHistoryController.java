package com.meiyou.bigwhale.controller;

import com.meiyou.bigwhale.common.Constant;
import com.meiyou.bigwhale.common.pojo.Msg;
import com.meiyou.bigwhale.data.domain.PageRequest;
import com.meiyou.bigwhale.dto.DtoScriptHistory;
import com.meiyou.bigwhale.entity.Schedule;
import com.meiyou.bigwhale.entity.ScriptHistory;
import com.meiyou.bigwhale.job.ScriptHistoryShellRunnerJob;
import com.meiyou.bigwhale.security.LoginUser;
import com.meiyou.bigwhale.service.ScheduleService;
import com.meiyou.bigwhale.service.ScriptHistoryService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/script_history")
public class ScriptHistoryController extends BaseController{

    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private ScriptHistoryService scriptHistoryService;
    @Autowired
    private ScheduleService scheduleService;

    @RequestMapping(value = "/page.api", method = RequestMethod.POST)
    public Msg page(@RequestBody DtoScriptHistory req) {
        LoginUser currentUser = getCurrentUser();
        if (!currentUser.isRoot()) {
            req.setCreateBy(currentUser.getId());
        }
        List<String> tokens = new ArrayList<>();
        if (StringUtils.isNotBlank(req.getContent())) {
            tokens.add("content?" + req.getContent());
        }
        if (req.getScheduleId() != null) {
            tokens.add("scheduleId=" + req.getScheduleId());
        }
        if (req.getScriptId() != null) {
            tokens.add("scriptId=" + req.getScriptId());
        }
        if (req.getStart() != null) {
            tokens.add("createTime>=" + dateFormat.format(req.getStart()));
        }
        if (req.getEnd() != null) {
            tokens.add("createTime<=" + dateFormat.format(req.getEnd()));
        }
        if (req.getCreateBy() != null) {
            tokens.add("createBy=" + req.getCreateBy());
        }
        if (req.getId() != null) {
            tokens.add("id=" + req.getId());
        }
        if (req.getScheduleTopNodeId() != null) {
            tokens.add("scheduleId=" + req.getScheduleId());
            tokens.add("scheduleTopNodeId=" + req.getScheduleTopNodeId());
            tokens.add("scheduleInstanceId=" + req.getScheduleInstanceId());
        }
        Page<DtoScriptHistory> dtoScriptHistoryPage = scriptHistoryService.pageByQuery(new PageRequest(req.pageNo - 1, req.pageSize, StringUtils.join(tokens, ";"),
                new Sort(Sort.Direction.DESC, "createTime", "id"))).map((scriptHistory) -> {
            DtoScriptHistory dtoScriptHistory = new DtoScriptHistory();
            BeanUtils.copyProperties(scriptHistory, dtoScriptHistory);
            String displayName;
            if (scriptHistory.getScriptId() != null) {
                if (scriptHistory.getScheduleId() != null) {
                    Schedule schedule = scheduleService.findById(scriptHistory.getScheduleId());
                    if (schedule != null) {
                        displayName = schedule.getName() + " - " +
                                scriptHistory.getScriptName();
                    } else {
                        displayName = "?" + " - " +
                                scriptHistory.getScriptName();
                    }
                    if (scriptHistory.getScheduleSupplement() != null && scriptHistory.getScheduleSupplement() ) {
                        displayName += "(补数)";
                    }
                } else {
                    displayName = "实时任务" + " - " +
                            scriptHistory.getScriptName();
                }
            } else {
                displayName = "Edit Test";
            }
            dtoScriptHistory.setDisplayName(displayName);
            return dtoScriptHistory;
        });
        return success(dtoScriptHistoryPage);
    }

    @RequestMapping(value = "/rerun.api", method = RequestMethod.POST)
    public Msg rerun(@RequestBody DtoScriptHistory req) {
        List<ScriptHistory> scriptHistories = scriptHistoryService.findByQuery(
                "scheduleId=" + req.getScheduleId() +
                        ";scheduleTopNodeId=" + req.getScheduleTopNodeId() +
                        ";scheduleInstanceId=" + req.getScheduleInstanceId());
        // 升序
        scriptHistories.sort(Comparator.comparing(ScriptHistory::getId));
        scriptHistories = scriptHistories.stream().filter(scriptHistory -> !scriptHistory.getScheduleSupplement()).collect(Collectors.toList());
        if (scriptHistories.isEmpty()) {
            return failed("补数节点不能重跑");
        }
        ScriptHistory scriptHistory = scriptHistories.get(0);
        if (scriptHistory.getCreateTime().after(new Date())) {
            return failed();
        }
        scriptHistory.resetState();
        scriptHistory.setScheduleOperateTime(new Date());
        scriptHistory.updateState(Constant.JobState.UN_CONFIRMED_);
        scriptHistory.updateState(Constant.JobState.WAITING_PARENT_);
        scriptHistory.updateState(Constant.JobState.INITED);
        scriptHistory = scriptHistoryService.save(scriptHistory);
        ScriptHistoryShellRunnerJob.build(scriptHistory);
        return success();
    }

}
