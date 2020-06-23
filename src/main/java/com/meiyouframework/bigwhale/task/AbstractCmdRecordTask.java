package com.meiyouframework.bigwhale.task;

import com.meiyouframework.bigwhale.common.Constant;
import com.meiyouframework.bigwhale.task.cmd.CmdRecordRunner;
import com.meiyouframework.bigwhale.util.MsgTools;
import com.meiyouframework.bigwhale.entity.*;
import com.meiyouframework.bigwhale.entity.auth.User;
import com.meiyouframework.bigwhale.service.*;
import com.meiyouframework.bigwhale.service.auth.UserService;
import com.meiyouframework.bigwhale.util.SpringContextUtils;
import org.apache.commons.lang.StringUtils;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;


/**
 * @author Suxy
 * @date 2019/9/11
 * @description file description
 */
public abstract class AbstractCmdRecordTask extends AbstractNoticeableTask {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractCmdRecordTask.class);

    /**
     * 提交子任务
     * @param cmdRecord
     * @param cmdRecordService
     * @param agentService
     */
    protected void submitSubCmdRecord(CmdRecord cmdRecord, CmdRecordService cmdRecordService, AgentService agentService, ScriptService scriptService) {
        if (StringUtils.isNotBlank(cmdRecord.getSchedulingId()) && StringUtils.isNotBlank(cmdRecord.getSubScriptIds())) {
            String [] subScriptIds = cmdRecord.getSubScriptIds().split(",");
            List<String> scriptIds = new ArrayList<>(subScriptIds.length);
            Collections.addAll(scriptIds, subScriptIds);
            String subScriptId = scriptIds.remove(0);
            if (StringUtils.isNotBlank(subScriptId)) {
                Script script = scriptService.findById(subScriptId);
                String agentId = script.getAgentId();
                if (StringUtils.isBlank(agentId)) {
                    Agent agent = agentService.getByClusterId(script.getClusterId());
                    if (agent != null) {
                        agentId = agent.getId();
                    }
                }
                CmdRecord record = CmdRecord.builder()
                        .parentId(cmdRecord.getId())
                        .uid(cmdRecord.getUid())
                        .scriptId(script.getId())
                        .createTime(new Date())
                        .content(script.getScript())
                        .timeOut(script.getTimeOut())
                        .status(Constant.EXEC_STATUS_UNSTART)
                        .agentId(agentId)
                        .clusterId(script.getClusterId())
                        .schedulingId(cmdRecord.getSchedulingId())
                        .args(cmdRecord.getArgs())
                        .build();
                if (!scriptIds.isEmpty()) {
                    record.setSubScriptIds(StringUtils.join(scriptIds, ","));
                }
                record = cmdRecordService.save(record);
                try {
                    CmdRecordRunner.build(record);
                } catch (SchedulerException e) {
                    LOGGER.error("schedule submit error", e);
                }
            }
        }
    }

}
