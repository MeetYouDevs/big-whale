package com.meiyouframework.bigwhale.entity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.meiyouframework.bigwhale.common.Constant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "scheduling")
public class Scheduling {

    @Id
    @GenericGenerator(name = "idGenerator", strategy = "uuid")
    @GeneratedValue(generator = "idGenerator")
    private String id;
    private String uid;
    private Integer type;
    /**
     * 多条数据用,分割
     */
    private String scriptIds;

    /**
     * 周期
     */
    private Integer cycle;
    private Integer intervals;
    private Integer minute;
    private Integer hour;
    /**
     * 多条数据用,分割
     */
    private String week;
    /**
     * cron表达式
     */
    private String cron;
    private Date startTime;
    private Date endTime;

    /* ---------------- 离线调度 start ---------------- */

    private String topology;
    /**
     * 可重复提交
     */
    private Boolean repeatSubmit;

    /* ---------------- 离线调度 end ---------------- */

    /* ---------------- 实时监控 start ---------------- */

    /**
     * 是否异常重启
     */
    private Boolean exRestart;
    /**
     * spark 挤压批次
     * flink 背压监控的任务阻塞次数
     */
    private Integer waitingBatches;
    private Boolean blockingRestart;

    /* ---------------- 实时监控 end ---------------- */

    private Boolean sendEmail;
    /**
     * 多条数据用,分割
     */
    private String dingdingHooks;

    private Date lastExecuteTime;
    private Date createTime;
    private Date updateTime;
    private Boolean enabled;

    public String generateCron() {
        if (StringUtils.isNotBlank(cron)) {
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

    public NodeData analyzeCurrentNode(String currentNodeId) {
        if (type == Constant.SCHEDULING_TYPE_STREAMING) {
            return null;
        }
        JSONObject jsonObject = JSON.parseObject(topology);
        JSONArray nodes = jsonObject.getJSONArray("nodes");
        for (Object node : nodes) {
            JSONObject nodeObj = (JSONObject) node;
            String nodeId = nodeObj.getString("id");
            NodeData data = JSON.parseObject(nodeObj.getString("data"), NodeData.class);
            if (nodeId.equals(currentNodeId)) {
                return data;
            }
        }
        return null;
    }

    public Map<String, NodeData> analyzeNextNode(String currentNodeId) {
        Map<String, NodeData> nodeIdToData = new HashMap<>();
        if (type == Constant.SCHEDULING_TYPE_STREAMING) {
            if (currentNodeId == null) {
                nodeIdToData.put(scriptIds, new NodeData(scriptIds, 0, 0));
            }
            return nodeIdToData;
        }
        JSONObject jsonObject = JSON.parseObject(topology);
        JSONArray nodes = jsonObject.getJSONArray("nodes");
        JSONArray lines = jsonObject.getJSONArray("lines");
        nodes.forEach(node -> {
            JSONObject nodeObj = (JSONObject) node;
            String nodeId = nodeObj.getString("id");
            NodeData data = JSON.parseObject(nodeObj.getString("data"), NodeData.class);
            nodeIdToData.put(nodeId, data);
        });
        List<String> toIds = new ArrayList<>();
        lines.forEach(line -> toIds.add(((JSONObject)line).getJSONObject("to").getString("id")));
        String rootNodeId = null;
        for (String id : nodeIdToData.keySet()) {
            if (!toIds.contains(id)) {
                rootNodeId = id;
                break;
            }
        }
        if (currentNodeId == null) {
            return Collections.singletonMap(rootNodeId, nodeIdToData.get(rootNodeId));
        } else {
            nodeIdToData.remove(rootNodeId);
            for (int i = 0; i < lines.size(); i ++) {
                JSONObject line = lines.getJSONObject(i);
                String fromId = line.getJSONObject("from").getString("id");
                if (!fromId.equals(currentNodeId)) {
                    nodeIdToData.remove(line.getJSONObject("to").getString("id"));
                }
            }
            return nodeIdToData;
        }
    }

    public static class NodeData {
        public String scriptId;
        public int retries;
        public int intervals;

        public NodeData(String scriptId, int retries, int intervals) {
            this.scriptId = scriptId;
            this.retries = retries;
            this.intervals = intervals;
        }
    }

}
