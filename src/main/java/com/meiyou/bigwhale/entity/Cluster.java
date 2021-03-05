package com.meiyou.bigwhale.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cluster")
public class Cluster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;
    private String yarnUrl;
    private String fsDefaultFs;
    private String fsWebhdfs;
    private String fsUser;
    private String fsDir;
    private Boolean defaultFileCluster;
    private Boolean flinkProxyUserEnabled;
    private String streamBlackNodeList;
    private String batchBlackNodeList;

}
