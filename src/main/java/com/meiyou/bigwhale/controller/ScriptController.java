package com.meiyou.bigwhale.controller;

import com.meiyou.bigwhale.common.Constant;
import com.meiyou.bigwhale.common.pojo.Msg;
import com.meiyou.bigwhale.dto.DtoScript;
import com.meiyou.bigwhale.entity.Script;
import com.meiyou.bigwhale.entity.ScriptHistory;
import com.meiyou.bigwhale.scheduler.job.ScriptJob;
import com.meiyou.bigwhale.security.LoginUser;
import com.meiyou.bigwhale.service.ScriptHistoryService;
import com.meiyou.bigwhale.service.ScriptService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;


@RestController
@RequestMapping("/script")
public class ScriptController extends BaseController {

    @Autowired
    private ScriptService scriptService;
    @Autowired
    private ScriptHistoryService scriptHistoryService;

    @RequestMapping(value = "/execute.api", method = RequestMethod.POST)
    public Msg execute(@RequestBody DtoScript req) {
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
        req.setId(null);
        req.setUpdateTime(now);
        req.setUpdateBy(currentUser.getId());
        Script script = new Script();
        BeanUtils.copyProperties(req, script);
        ScriptHistory scriptHistory = scriptService.generateHistory(script);
        if (Constant.JobState.SUBMIT_WAIT.equals(scriptHistory.getState())) {
            scriptHistory.updateState(Constant.JobState.SUBMITTING);
            scriptHistory = scriptHistoryService.save(scriptHistory);
            ScriptJob.build(scriptHistory);
        }
        return success(scriptHistory);
    }

}