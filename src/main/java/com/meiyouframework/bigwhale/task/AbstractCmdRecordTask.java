package com.meiyouframework.bigwhale.task;

import com.meiyouframework.bigwhale.common.Constant;
import com.meiyouframework.bigwhale.task.cmd.CmdRecordRunner;
import com.meiyouframework.bigwhale.entity.*;
import com.meiyouframework.bigwhale.service.*;
import org.apache.commons.lang.time.DateUtils;
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
     * 重试当前节点
     * @param cmdRecord
     * @param scheduling
     */
    protected void retryCurrentNode(CmdRecord cmdRecord, Scheduling scheduling) {
        if (scheduling != null && scheduling.getType() == Constant.SCHEDULING_TYPE_BATCH) {
            //在上一次脚本任务链未执行完毕的情况下，更新过调度，则跳过余下脚本任务
            if (cmdRecord.getCreateTime().before(scheduling.getUpdateTime())) {
                return;
            }
            Scheduling.NodeData nodeData = scheduling.analyzeCurrentNode(cmdRecord.getSchedulingNodeId());
            if (nodeData.retries == 0) {
                return;
            }
            if (cmdRecord.getRetryNum() != null && cmdRecord.getRetryNum() >= nodeData.retries) {
                return;
            }
            Date startAt = DateUtils.addMinutes(new Date(), nodeData.intervals);
            CmdRecord record = CmdRecord.builder()
                    .parentId(cmdRecord.getParentId())
                    .uid(cmdRecord.getUid())
                    .scriptId(cmdRecord.getScriptId())
                    .createTime(startAt)
                    .content(cmdRecord.getContent())
                    .timeout(cmdRecord.getTimeout())
                    .status(Constant.EXEC_STATUS_UNSTART)
                    .agentId(cmdRecord.getAgentId())
                    .clusterId(cmdRecord.getClusterId())
                    .schedulingId(cmdRecord.getSchedulingId())
                    .schedulingInstanceId(cmdRecord.getSchedulingInstanceId())
                    .schedulingNodeId(cmdRecord.getSchedulingNodeId())
                    .retryNum(cmdRecord.getRetryNum() != null ? cmdRecord.getRetryNum() + 1 : 1)
                    .args(cmdRecord.getArgs())
                    .build();
            record = cmdRecordService.save(record);
            try {
                CmdRecordRunner.build(record, startAt);
            } catch (SchedulerException e) {
                LOGGER.error("schedule submit error", e);
            }
        }
    }

    /**
     * 提交下一个节点
     * @param cmdRecord
     * @param scheduling
     * @param scriptService
     */
    protected void submitNextNode(CmdRecord cmdRecord, Scheduling scheduling, ScriptService scriptService) {
        if (scheduling != null && scheduling.getType() == Constant.SCHEDULING_TYPE_BATCH) {
            //在上一次脚本任务链未执行完毕的情况下，更新过调度，则跳过余下脚本任务
            if (cmdRecord.getCreateTime().before(scheduling.getUpdateTime())) {
                return;
            }
            Map<String, Scheduling.NodeData> nodeIdToData = scheduling.analyzeNextNode(cmdRecord.getSchedulingNodeId());
            if (nodeIdToData.isEmpty()) {
                return;
            }
            for (Map.Entry<String, Scheduling.NodeData> entry : nodeIdToData.entrySet()) {
                String nodeId = entry.getKey();
                String scriptId = entry.getValue().scriptId;
                Script script = scriptService.findById(scriptId);
                CmdRecord record = CmdRecord.builder()
                        .parentId(cmdRecord.getId())
                        .uid(cmdRecord.getUid())
                        .scriptId(scriptId)
                        .createTime(new Date())
                        .content(script.getScript())
                        .timeout(script.getTimeout())
                        .status(Constant.EXEC_STATUS_UNSTART)
                        .agentId(script.getAgentId())
                        .clusterId(script.getClusterId())
                        .schedulingId(cmdRecord.getSchedulingId())
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
