package com.meiyouframework.bigwhale.task;

import com.meiyouframework.bigwhale.common.Constant;
import com.meiyouframework.bigwhale.task.cmd.CmdRecordRunner;
import com.meiyouframework.bigwhale.entity.*;
import com.meiyouframework.bigwhale.service.*;
import com.meiyouframework.bigwhale.util.SpringContextUtils;
import org.apache.commons.lang.StringUtils;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * @author Suxy
 * @date 2019/9/11
 * @description file description
 */
public abstract class AbstractCmdRecordTask extends AbstractNoticeableTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCmdRecordTask.class);

    protected CmdRecordService cmdRecordService;
    protected SchedulingService schedulingService;

    protected AbstractCmdRecordTask () {
        cmdRecordService = SpringContextUtils.getBean(CmdRecordService.class);
        schedulingService = SpringContextUtils.getBean(SchedulingService.class);
    }

    /**
     * 提交下一个任务
     * @param cmdRecord
     * @param scheduling
     * @param agentService
     * @param scriptService
     */
    protected void submitNextCmdRecord(CmdRecord cmdRecord, Scheduling scheduling, AgentService agentService, ScriptService scriptService) {
        if (scheduling != null) {
            //在上一次脚本任务链未执行完毕的情况下，更新过调度，则跳过余下脚本任务
            if (cmdRecord.getCreateTime().before(scheduling.getUpdateTime())) {
                return;
            }
            if (StringUtils.isBlank(cmdRecord.getSubScriptIds())) {
                return;
            }
            String [] items = cmdRecord.getSubScriptIds().split(",");
            List<String> subScriptIds = new ArrayList<>(items.length);
            Collections.addAll(subScriptIds, items);
            String subScriptId = subScriptIds.remove(0);
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
                    .uid(scheduling.getUid())
                    .scriptId(script.getId())
                    .createTime(new Date())
                    .content(script.getScript())
                    .timeout(script.getTimeout())
                    .status(Constant.EXEC_STATUS_UNSTART)
                    .agentId(agentId)
                    .clusterId(script.getClusterId())
                    .schedulingId(cmdRecord.getSchedulingId())
                    .schedulingInstanceId(cmdRecord.getSchedulingInstanceId())
                    .args(cmdRecord.getArgs())
                    .build();
            if (!subScriptIds.isEmpty()) {
                record.setSubScriptIds(StringUtils.join(subScriptIds, ","));
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
