package com.meiyou.bigwhale.entity;

import com.meiyou.bigwhale.common.Constant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "script_history")
public class ScriptHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    private Integer timeout;
    private String content;
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

    public void updateState(String state) {
        this.state = state;
        if (this.steps == null) {
            this.steps = "[\"" + state + "\"]";
        } else {
            if (!this.steps.contains(state)) {
                this.steps = this.steps.split("]")[0] + ",\"" + state + "\"]";
            }
        }
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
