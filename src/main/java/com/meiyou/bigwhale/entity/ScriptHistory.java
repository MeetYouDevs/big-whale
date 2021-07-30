package com.meiyou.bigwhale.entity;

import com.meiyou.bigwhale.common.Constant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
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
     * 监控
     */
    private Integer monitorId;

    /**
     * 调度
     */
    private Integer scheduleId;
    /**
     * 拓扑节点ID
     */
    private String scheduleTopNodeId;
    private String scheduleInstanceId;
    private String scheduleFailureHandle;
    private Boolean scheduleRunnable;
    private Boolean scheduleRetry;
    private Boolean scheduleEmpty;
    private Boolean scheduleRerun;
    private String previousScheduleTopNodeId;

    private Integer scriptId;
    private String scriptName;
    private String scriptType;
    private Integer clusterId;
    private Integer agentId;
    private Integer timeout;
    private String content;
    private String state;
    private String steps;
    private String outputs;
    private String errors;
    private Date createTime;
    private Integer createBy;
    private Date businessTime;
    private Date delayTime;
    private Date submitTime;
    private Date startTime;
    private Date finishTime;

    /* ----- yarn 相关字段 ----- */
    /**
     * user;queue;name
     */
    private String jobParams;
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
        if (state.equals(Constant.JobState.SUBMITTING)) {
            this.submitTime = new Date();
        }
    }

    public void reset() {
        this.id = null;
        this.scheduleFailureHandle = null;
        this.state = null;
        this.steps = null;
        this.outputs = null;
        this.errors = null;
        this.createTime = null;
        this.delayTime = null;
        this.submitTime = null;
        this.startTime = null;
        this.finishTime = null;
        this.jobId = null;
        this.jobUrl = null;
        this.jobFinalStatus = null;
    }

    public void updateParams(String yarnProxyUser, String yarnQueue, String yarnName) {
        this.jobParams = yarnProxyUser + ";" + yarnQueue + ";" + yarnName;
    }

    public boolean isRunning() {
        return  Constant.JobState.TIME_WAIT_.equals(state) ||
                Constant.JobState.SUBMIT_WAIT.equals(state) ||
                Constant.JobState.SUBMITTING.equals(state) ||
                Constant.JobState.SUBMITTED.equals(state) ||
                Constant.JobState.ACCEPTED.equals(state) ||
                Constant.JobState.RUNNING.equals(state);
    }

}
