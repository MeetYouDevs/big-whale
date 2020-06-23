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
@Table(name = "yarn_app")
public class YarnApp {

    @Id
    @GenericGenerator(name="idGenerator", strategy="uuid")
    @GeneratedValue(generator="idGenerator")
    private String id;
    private String uid;
    private String scriptId;
    private String clusterId;
    private Date updateTime;
    private String appId;
    private String user;
    private String name;
    private String queue;
    private String state;
    private String finalStatus;
    private String trackingUrl;
    private String applicationType;
    private Date startedTime;
}
