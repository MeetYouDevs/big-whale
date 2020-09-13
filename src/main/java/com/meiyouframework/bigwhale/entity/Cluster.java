package com.meiyouframework.bigwhale.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cluster")
public class Cluster {

    @Id
    @GenericGenerator(name = "idGenerator", strategy = "uuid")
    @GeneratedValue(generator = "idGenerator")
    private String id;
    private String name;
    private String yarnUrl;
    private String fsDefaultFs;
    private String fsWebhdfs;
    private String fsUser;
    private String fsDir;
    private Boolean defaultFileCluster;
    private Boolean flinkProxyUserEnabled;
    private String streamingBlackNodeList;
    private String batchBlackNodeList;

}
