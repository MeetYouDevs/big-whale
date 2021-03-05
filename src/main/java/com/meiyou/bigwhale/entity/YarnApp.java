package com.meiyou.bigwhale.entity;

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
@Table(name = "yarn_app")
public class YarnApp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer clusterId;
    private Integer userId;
    private String appId;
    private String user;
    private String name;
    private String queue;
    private String state;
    private String finalStatus;
    private String trackingUrl;
    private String applicationType;
    private Date startedTime;
    @Transient
    private Integer allocatedMB;
    private Date refreshTime;

}
