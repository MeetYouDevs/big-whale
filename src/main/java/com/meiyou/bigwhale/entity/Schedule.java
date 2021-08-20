package com.meiyou.bigwhale.entity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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

    public Topology.Node analyzeCurrentNode(String currentNodeId) {
        Topology top = JSON.parseObject(topology, Topology.class);
        for (Topology.Node node : top.nodes) {
            if (node.id.equals(currentNodeId)) {
                return node;
            }
        }
        return null;
    }

    /**
     * @param currentNodeId
     * @return {nodeId : Node}
     */
    public Map<String, Topology.Node> analyzeNextNode(String currentNodeId) {
        Map<String, Topology.Node> nodeIdToObj = new HashMap<>();
        Topology top = JSON.parseObject(topology, Topology.class);
        top.nodes.forEach(node -> nodeIdToObj.put(node.id, node));
        List<String> toIds = new ArrayList<>();
        top.lines.forEach(line -> toIds.add(line.toId()));
        String rootNodeId = null;
        for (Topology.Node node : top.nodes) {
            if (!toIds.contains(node.id)) {
                rootNodeId = node.id;
                break;
            }
        }
        if (currentNodeId == null) {
            return Collections.singletonMap(rootNodeId, nodeIdToObj.get(rootNodeId));
        } else {
            nodeIdToObj.remove(rootNodeId);
            top.lines.forEach(line -> {
                if (!line.fromId().equals(currentNodeId)) {
                    nodeIdToObj.remove(line.toId());
                }
            });
            return nodeIdToObj;
        }
    }

    public static class Topology {

        public List<Node> nodes;
        public List<Line> lines;

        public Topology(List<Node> nodes, List<Line> lines) {
            this.nodes = nodes;
            this.lines = lines;
        }

        public static class Node {
            public String id;
            public JSONObject data;

            public Node(String id, JSONObject data) {
                this.id = id;
                this.data = data;
            }

            public int retries() {
                return data.getIntValue("retries");
            }

            public int intervals() {
                return data.getIntValue("intervals");
            }
        }

        public static class Line {
            public String id;
            public JSONObject from;
            public JSONObject to;

            public Line(String id, JSONObject from, JSONObject to) {
                this.id = id;
                this.from = from;
                this.to = to;
            }

            public String fromId() {
                return from.getString("id");
            }

            public String toId() {
                return to.getString("id");
            }
        }
    }

}
