package com.meiyouframework.bigwhale.task;

import com.meiyouframework.bigwhale.common.Constant;
import com.meiyouframework.bigwhale.task.cmd.CmdRecordRunner;
import com.meiyouframework.bigwhale.entity.*;
import com.meiyouframework.bigwhale.service.*;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;


/**
 * @author Suxy
 * @date 2019/9/11
 * @description file description
 */
public abstract class AbstractCmdRecordTask extends AbstractNoticeableTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCmdRecordTask.class);

    @Autowired
    protected CmdRecordService cmdRecordService;
    @Autowired
    protected SchedulingService schedulingService;

    /**
     * 提交下一个任务
     * @param cmdRecord
     * @param scheduling
     * @param scriptService
     */
    protected void submitNextCmdRecord(CmdRecord cmdRecord, Scheduling scheduling, ScriptService scriptService) {
        if (scheduling != null) {
            //在上一次脚本任务链未执行完毕的情况下，更新过调度，则跳过余下脚本任务
            if (cmdRecord.getCreateTime().before(scheduling.getUpdateTime())) {
                return;
            }
            Map<String, String> nodeIdToScriptId = scheduling.analyzeNextNode(cmdRecord.getSchedulingNodeId());
            if (nodeIdToScriptId.isEmpty()) {
                return;
            }
            for (Map.Entry<String, String> entry : nodeIdToScriptId.entrySet()) {
                String nodeId = entry.getKey();
                String scriptId = entry.getValue();
                Script script = scriptService.findById(scriptId);
                CmdRecord record = CmdRecord.builder()
                        .parentId(cmdRecord.getId())
                        .uid(scheduling.getUid())
                        .scriptId(scriptId)
                        .createTime(new Date())
                        .content(script.getScript())
                        .timeout(script.getTimeout())
                        .status(Constant.EXEC_STATUS_UNSTART)
                        .agentId(script.getAgentId())
                        .clusterId(script.getClusterId())
                        .schedulingId(scheduling.getId())
                        .schedulingInstanceId(cmdRecord.getSchedulingInstanceId())
                        .schedulingNodeId(nodeId)
                        .args(cmdRecord.getArgs())
                        .build();
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
