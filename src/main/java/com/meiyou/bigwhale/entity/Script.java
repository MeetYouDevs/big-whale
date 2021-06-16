package com.meiyou.bigwhale.entity;

import com.meiyou.bigwhale.common.Constant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

/**
 * @author meiyou big data group
 * @date 2019/12/12
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "script")
public class Script {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;
    private String description;
    private String type;
    private Integer monitorId;
    /**
     * 列表查询字段
     */
    private Boolean monitorEnabled;
    private Integer scheduleId;
    /**
     * 拓扑节点ID
     */
    private String scheduleTopNodeId;
    private Integer clusterId;
    private Integer agentId;
    private Integer timeout;
    private String content;
    private String input;
    private String output;
    private Date createTime;
    private Integer createBy;
    private Date updateTime;
    private Integer updateBy;

    /**
     * yarn应用属性
     */
    private String user;
    private String queue;
    private String app;

    public boolean isBatch() {
        return !Constant.ScriptType.SPARK_STREAM.equals(type) &&
                !Constant.ScriptType.FLINK_STREAM.equals(type);
    }

    public boolean isYarn() {
        return Constant.ScriptType.SPARK_BATCH.equals(type) ||
                Constant.ScriptType.SPARK_STREAM.equals(type) ||
                Constant.ScriptType.FLINK_BATCH.equals(type) ||
                Constant.ScriptType.FLINK_STREAM.equals(type);
    }

}
