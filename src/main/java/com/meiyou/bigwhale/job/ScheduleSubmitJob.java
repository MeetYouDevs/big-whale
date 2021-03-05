package com.meiyou.bigwhale.job;

import com.meiyou.bigwhale.common.Constant;
import com.meiyou.bigwhale.entity.ScriptHistory;
import com.meiyou.bigwhale.entity.ScheduleSnapshot;
import com.meiyou.bigwhale.service.ScriptHistoryService;
import com.meiyou.bigwhale.service.ScheduleSnapshotService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Suxy
 * @date 2020/4/23
 * @description file description
 */
@DisallowConcurrentExecution
public class ScheduleSubmitJob implements Job {

    @Autowired
    private ScriptHistoryService scriptHistoryService;
    @Autowired
    private ScheduleSnapshotService scheduleSnapshotService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        List<ScriptHistory> scriptHistories = scriptHistoryService.findByQuery("state=" + Constant.JobState.WAITING_PARENT_,
                new Sort(Sort.Direction.ASC, "createTime"));
        for (ScriptHistory scriptHistory : scriptHistories) {
            ScheduleSnapshot scheduleSnapshot = scheduleSnapshotService.findById(scriptHistory.getScheduleSnapshotId());
            ScheduleSnapshot.Topology.Node previousNode = scheduleSnapshot.analyzePreviousNode(scriptHistory.getScheduleTopNodeId());
            if (previousNode == null) {
                // 根节点
                ScriptHistoryShellRunnerJob.build(scriptHistory);
            } else {
                String previousNodeState = getPreviousNodeState(scriptHistory.getScheduleId(), previousNode, scriptHistory.getScheduleInstanceId());
                switch (previousNodeState) {
                    case Constant.JobState.SUCCEEDED:
                        scriptHistory.updateState(Constant.JobState.INITED);
                        scriptHistoryService.save(scriptHistory);
                        ScriptHistoryShellRunnerJob.build(scriptHistory);
                        break;
                    case Constant.JobState.FAILED:
                        scriptHistory.updateState(Constant.JobState.PARENT_FAILED_);
                        scriptHistory.setFinishTime(new Date());
                        scriptHistoryService.save(scriptHistory);
                        markNextNodeFailed(scheduleSnapshot, scriptHistory.getScheduleTopNodeId(), scriptHistory.getScheduleInstanceId());
                        break;
                    case Constant.JobState.RUNNING:
                        break;
                    default:
                }
            }
        }
    }

    /**
     * @param scheduleId
     * @param previousNode
     * @param scheduleInstanceId
     * @return 0 成功 1 失败 2 执行中
     */
    private String getPreviousNodeState(Integer scheduleId, ScheduleSnapshot.Topology.Node previousNode, String scheduleInstanceId) {
        List<ScriptHistory> previousScriptHistories = scriptHistoryService.findByQuery("scheduleId=" + scheduleId +
                ";scheduleTopNodeId=" + previousNode.id +
                ";scheduleInstanceId=" + scheduleInstanceId);
        for (ScriptHistory previousScriptHistory : previousScriptHistories) {
            if (previousScriptHistory.isRunning()) {
                return Constant.JobState.RUNNING;
            }
            if (Constant.JobState.SUCCEEDED.equals(previousScriptHistory.getState())) {
                return Constant.JobState.SUCCEEDED;
            }
        }
        previousScriptHistories = previousScriptHistories.stream().filter(previousScriptHistory -> previousScriptHistory.getScheduleRetryNum() != null).collect(Collectors.toList());
        if (previousScriptHistories.size() < previousNode.retries()) {
            return Constant.JobState.RUNNING;
        }
        return Constant.JobState.FAILED;
    }

    private void markNextNodeFailed(ScheduleSnapshot scheduleSnapshot, String scheduleTopNodeId, String scheduleInstanceId) {
        Map<String, ScheduleSnapshot.Topology.Node> nextNodeIdToObj = scheduleSnapshot.analyzeNextNode(scheduleTopNodeId);
        for (Map.Entry<String, ScheduleSnapshot.Topology.Node> entry : nextNodeIdToObj.entrySet()) {
            ScriptHistory nextScriptHistory = scriptHistoryService.findOneByQuery("scheduleId=" + scheduleSnapshot.getScheduleId() +
                    ";scheduleTopNodeId=" + entry.getKey() +
                    ";scheduleInstanceId=" + scheduleInstanceId);
            nextScriptHistory.updateState(Constant.JobState.PARENT_FAILED_);
            nextScriptHistory.setFinishTime(new Date());
            scriptHistoryService.save(nextScriptHistory);
            markNextNodeFailed(scheduleSnapshot, nextScriptHistory.getScheduleTopNodeId(), scheduleInstanceId);
        }
    }

}
