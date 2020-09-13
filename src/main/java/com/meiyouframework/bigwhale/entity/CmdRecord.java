package com.meiyouframework.bigwhale.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cmd_record")
public class CmdRecord {

    @Id
    @GenericGenerator(name = "idGenerator", strategy = "uuid")
    @GeneratedValue(generator = "idGenerator")
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
     * 离线调度实例ID
     */
    private String schedulingInstanceId;
    /**
     * 离线调度节点ID
     */
    private String schedulingNodeId;
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
    /**
     * for batch job
     */
    private String jobFinalStatus;
    private String url;

}
