package com.meiyouframework.bigwhale.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoCmdRecord extends AbstractPageDto {

    private String id;
    private String parentId;
    private String scriptId;
    /**
     * 脚本执行状态
     */
    private Integer status;
    private String agentId;
    private String clusterId;
    private String agentInstance;
    private String uid;
    /**
     * 离线调度ID
     */
    private String schedulingId;
    /**
     * 任务调度实例ID（批处理）
     */
    private String schedulingInstanceId;
    /**
     * 任务调度节点ID（批处理）
     */
    private String schedulingNodeId;
    /**
     * 任务调度重试序号（批处理）
     */
    private Integer retryNum;

    private String content;
    private Integer timeout;
    private String outputs;
    private String errors;
    private Date createTime;
    private Date startTime;
    private Date finishTime;
    private String args;

    /**
     * for spark or flink job
     */
    private String jobId;
    private String jobUrl;
    /**
     * for batch job
     */
    private String jobFinalStatus;

    /**
     * 查询属性
     */
    private Date start;
    private Date end;

    @Override
    public String validate() {
        return null;
    }
}
