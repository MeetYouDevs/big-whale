package com.meiyou.bigwhale.job;

import com.meiyou.bigwhale.common.Constant;
import com.meiyou.bigwhale.entity.ScriptHistory;
import com.meiyou.bigwhale.entity.ScheduleSnapshot;
import com.meiyou.bigwhale.service.ScriptHistoryService;
import com.meiyou.bigwhale.service.ScheduleSnapshotService;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;


/**
 * @author Suxy
 * @date 2019/9/11
 * @description file description
 */
public abstract class AbstractRetryableJob extends AbstractNoticeableJob {

    @Autowired
    protected ScriptHistoryService scriptHistoryService;
    @Autowired
    protected ScheduleSnapshotService scheduleSnapshotService;

    /**
     * 重试当前节点
     * @param scriptHistory
     * @param errorType
     */
    protected void retryCurrentNode(ScriptHistory scriptHistory, String errorType) {
        notice(scriptHistory, errorType);
        boolean retryable = scriptHistory.getScheduleId() != null &&
                (scriptHistory.getScheduleHistoryMode() == null || Constant.HistoryMode.RETRY.equals(scriptHistory.getScheduleHistoryMode())) &&
                !"UNKNOWN".equals(scriptHistory.getJobFinalStatus());
        if (retryable) {
            ScheduleSnapshot scheduleSnapshot = scheduleSnapshotService.findById(scriptHistory.getScheduleSnapshotId());
            ScheduleSnapshot.Topology.Node node = scheduleSnapshot.analyzeCurrentNode(scriptHistory.getScheduleTopNodeId());
            if (node.retries() == 0) {
                return;
            }
            if (scriptHistory.getScheduleRetryNum() != null && scriptHistory.getScheduleRetryNum() >= node.retries()) {
                return;
            }
            Date startAt = DateUtils.addMinutes(new Date(), node.intervals());
            ScriptHistory retryScriptHistory = ScriptHistory.builder()
                    .scheduleId(scriptHistory.getScheduleId())
                    .scheduleTopNodeId(scriptHistory.getScheduleTopNodeId())
                    .scheduleSnapshotId(scriptHistory.getScheduleSnapshotId())
                    .scheduleInstanceId(scriptHistory.getScheduleInstanceId())
                    .scheduleRetryNum(scriptHistory.getScheduleRetryNum() != null ? scriptHistory.getScheduleRetryNum() + 1 : 1)
                    .scheduleHistoryMode(Constant.HistoryMode.RETRY)
                    .scheduleHistoryTime(startAt)
                    .scriptId(scriptHistory.getScriptId())
                    .scriptType(scriptHistory.getScriptType())
                    .agentId(scriptHistory.getAgentId())
                    .clusterId(scriptHistory.getClusterId())
                    .timeout(scriptHistory.getTimeout())
                    .content(scriptHistory.getContent())
                    .createTime(scriptHistory.getCreateTime())
                    .createBy(scriptHistory.getCreateBy())
                    .build();
            retryScriptHistory.updateState(Constant.JobState.UN_CONFIRMED_);
            retryScriptHistory.updateState(Constant.JobState.WAITING_PARENT_);
            retryScriptHistory.updateState(Constant.JobState.INITED);
            retryScriptHistory = scriptHistoryService.save(retryScriptHistory);
            ScriptHistoryShellRunnerJob.build(retryScriptHistory, startAt);
        }
    }

}
