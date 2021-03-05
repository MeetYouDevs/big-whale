package com.meiyou.bigwhale.dto;

import com.meiyou.bigwhale.common.Constant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoScriptHistory extends AbstractPageDto {

    private Integer id;
    /**
     * 调度
     */
    private Integer scheduleId;
    /**
     * 拓扑节点ID
     */
    private String scheduleTopNodeId;
    private Integer scheduleSnapshotId;
    private String scheduleInstanceId;
    private Integer scheduleRetryNum;
    /**
     * 历史模式
     * retry 重试 rerun 重跑 supplement 补数
     */
    private String scheduleHistoryMode;
    private Date scheduleHistoryTime;

    /**
     * 监控
     */
    private Integer monitorId;

    private Integer scriptId;
    private String scriptType;
    private Integer agentId;
    private Integer clusterId;
    private String content;
    private Integer timeout;
    private String outputs;
    private String errors;
    private Date createTime;
    private Integer createBy;
    private Date startTime;
    private Date finishTime;
    private String state;
    private String steps;

    /**
     * for spark or flink job
     */
    private String jobId;
    private String jobUrl;
    private String jobFinalStatus;


    /**
     * ${调度/实时任务 - 节点名}
     */
    private String displayName;

    /**
     * 查询属性
     */
    private Date start;
    private Date end;

    @Override
    public String validate() {
        return null;
    }

    public boolean isRunning() {
        return  Constant.JobState.WAITING_PARENT_.equals(state) ||
                Constant.JobState.INITED.equals(state) ||
                Constant.JobState.SUBMITTING.equals(state) ||
                Constant.JobState.SUBMITTED.equals(state) ||
                Constant.JobState.ACCEPTED.equals(state) ||
                Constant.JobState.RUNNING.equals(state);
    }
}
