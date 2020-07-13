package com.meiyouframework.bigwhale.entity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
    private Date createTime;
    private Date updateTime;
    private Date startTime;
    private Date endTime;
    private Date lastExecuteTime;
    private Integer status;
    private String uid;
    private String topology;
    /**
     * 多条数据用,分割
     */
    private String scriptIds;

    /**
     * 可重复提交
     */
    private Boolean repeatSubmit;
    private Boolean sendMail;
    /**
     * 多条数据用,分割
     */
    private String dingdingHooks;

    public Map<String, String> analyzeNextNode(String currentNodeId) {
        Map<String, String> nodeIdToScriptId = new HashMap<>();
        if (topology == null) {
            return nodeIdToScriptId;
        }
        JSONObject jsonObject = JSON.parseObject(topology);
        JSONArray nodes = jsonObject.getJSONArray("nodes");
        JSONArray lines = jsonObject.getJSONArray("lines");
        nodes.forEach(node -> nodeIdToScriptId.put(((JSONObject)node).getString("id"), ((JSONObject)node).getString("data")));
        List<String> toIds = new ArrayList<>();
        lines.forEach(line -> toIds.add(((JSONObject)line).getJSONObject("to").getString("id")));
        String rootNodeId = null;
        for (String id : nodeIdToScriptId.keySet()) {
            if (!toIds.contains(id)) {
                rootNodeId = id;
                break;
            }
        }
        if (currentNodeId == null) {
            return Collections.singletonMap(rootNodeId, nodeIdToScriptId.get(rootNodeId));
        } else {
            nodeIdToScriptId.remove(rootNodeId);
            for (int i = 0; i < lines.size(); i ++) {
                JSONObject line = lines.getJSONObject(i);
                String fromId = line.getJSONObject("from").getString("id");
                if (!fromId.equals(currentNodeId)) {
                    nodeIdToScriptId.remove(line.getJSONObject("to").getString("id"));
                }
            }
            return nodeIdToScriptId;
        }
    }
}
