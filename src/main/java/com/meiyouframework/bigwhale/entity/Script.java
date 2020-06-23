package com.meiyouframework.bigwhale.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

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
    @GenericGenerator(name = "idGenerator", strategy = "uuid")
    @GeneratedValue(generator = "idGenerator")
    private String id;
    private String name;
    private String description;
    private Integer type;
    private Integer timeOut;
    private String script;
    private String input;
    private String output;
    private String agentId;
    private String clusterId;
    private String uid;
    private Date createTime;
    private Date updateTime;

    /**
     * yarn应用属性
     */
    private String user;
    private String queue;
    private String app;

    @Transient
    private Integer totalMemory;
    @Transient
    private Integer totalCores;
}
