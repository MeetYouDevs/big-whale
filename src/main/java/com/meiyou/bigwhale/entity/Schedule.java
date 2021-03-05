package com.meiyou.bigwhale.entity;

import com.alibaba.fastjson.JSON;
import com.meiyou.bigwhale.common.Constant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "schedule")
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String name;
    private String description;
    private Integer cycle;
    private Integer intervals;
    private Integer minute;
    private Integer hour;
    /**
     * 多条数据用,分割
     */
    private String week;
    private String cron;
    private Date startTime;
    private Date endTime;
    private String topology;
    private Boolean sendEmail;
    /**
     * 多条数据用,分割
     */
    private String dingdingHooks;
    private Boolean enabled;
    private Date realFireTime;
    private Date needFireTime;
    private Date nextFireTime;
    private Date createTime;
    private Integer createBy;
    private Date updateTime;
    private Integer updateBy;
    private String keyword;

    public String generateCron() {
        if (cron != null) {
            return cron;
        } else {
            String cron = null;
            if (cycle == Constant.TIMER_CYCLE_MINUTE) {
                cron = "0 */" + intervals + " * * * ? *";
            } else if (cycle == Constant.TIMER_CYCLE_HOUR) {
                cron = "0 " + minute + " * * * ? *";
            } else if (cycle == Constant.TIMER_CYCLE_DAY) {
                cron = "0 " + minute + " " + hour + " * * ? *";
            } else if (cycle == Constant.TIMER_CYCLE_WEEK) {
                cron = "0 " + minute + " " + hour + " ? * " + week + " *";
            }
            if (cron == null) {
                throw new IllegalArgumentException("cron expression is incorrect");
            }
            return cron;
        }
    }

    public String generateCompareTopology() {
        ScheduleSnapshot.Topology top = JSON.parseObject(topology, ScheduleSnapshot.Topology.class);
        List<Map<String, String>> nodes = new ArrayList<>();
        List<Map<String, String>> lines = new ArrayList<>();
        top.nodes.forEach(node -> {
            Map<String, String> nodeMap = new HashMap<>();
            nodeMap.put("id", node.id);
            nodes.add(nodeMap);
        });
        top.lines.forEach(line -> {
            Map<String, String> lineMap = new HashMap<>();
            lineMap.put("id", line.id);
            lineMap.put("fromId", line.fromId());
            lineMap.put("toId", line.toId());
            lines.add(lineMap);
        });
        nodes.sort(Comparator.comparing(node -> node.get("id")));
        lines.sort(Comparator.comparing(line -> line.get("id")));
        Map<String, Object> topMap = new HashMap<>();
        topMap.put("nodes", nodes);
        topMap.put("lines", lines);
        return JSON.toJSONString(topMap);
    }

}
