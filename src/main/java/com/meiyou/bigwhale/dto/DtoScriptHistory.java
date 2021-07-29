package com.meiyou.bigwhale.dto;

import com.meiyou.bigwhale.common.Constant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.regex.Pattern;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoScriptHistory extends AbstractPageDto {

    private static final Pattern PROXY_USER_PATTERN = Pattern.compile("proxy user: ([\\w-.,]+)");

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
    private Boolean scheduleSupplement;
    private Date scheduleOperateTime;
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
