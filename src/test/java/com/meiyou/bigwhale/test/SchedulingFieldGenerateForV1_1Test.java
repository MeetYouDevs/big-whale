//package com.meiyouframework.bigwhale.test;
//
//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.JSONArray;
//import com.alibaba.fastjson.JSONObject;
//import com.meiyouframework.bigwhale.entity.Scheduling;
//import com.meiyouframework.bigwhale.entity.Script;
//import com.meiyouframework.bigwhale.service.SchedulingService;
//import com.meiyouframework.bigwhale.service.ScriptService;
//import org.apache.commons.lang.StringUtils;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.junit4.SpringRunner;
//
//import java.util.*;
//
///**
// * @author Suxy
// * @date 2020/7/10
// * @description v1.0离线调度迁移到v1.1
// * 1. 全选，打开文件注释
// * 2. 在{@link com.meiyouframework.bigwhale.entity.Scheduling}中添加以下代码
// *      private String scriptId;
// *      private String subScriptIds;
// * 3. 查看big-whale.sql，注释相应的SQL语句后再运行该SQL文件
// * 4. 备份scheduling表（防止更新过程出现错误）
// * 5. 修改相应的@ActiveProfiles
// * 6. 确认相应的配置文件中数据库的配置信息
// * 7. 运行此测试
// * 8. 删除第2步中添加的代码
// * 9. 执行第3步中注释的SQL语句
// * 10. 执行SQL: UPDATE `scheduling` SET `last_execute_time` = Null;
// * 11. 升级完毕
// */
//@SpringBootTest
//@RunWith(SpringRunner.class)
//@ActiveProfiles("dev")
//public class SchedulingFieldGenerateForV1_1Test {
//
//    private final Map<Integer, String> scriptIconClass = new HashMap<>();
//    private final Map<Integer, Integer> scriptRectWidth = new HashMap<>();
//    private final Map<Integer, Integer> scriptRectHeight = new HashMap<>();
//    private final Map<Integer, String> scriptBox = new HashMap<>();
//    @Autowired
//    private SchedulingService schedulingService;
//    @Autowired
//    private ScriptService scriptService;
//
//    @Before
//    public void before() {
//        scriptIconClass.put(0, "\ue73e");
//        scriptIconClass.put(2, "\ue603");
//        scriptIconClass.put(4, "\ue61d");
//        scriptRectWidth.put(0, 80);
//        scriptRectWidth.put(2, 180);
//        scriptRectWidth.put(4, 100);
//        scriptRectHeight.put(0, 100);
//        scriptRectHeight.put(2, 60);
//        scriptRectHeight.put(4, 100);
//        scriptBox.put(0, "file");
//        scriptBox.put(2, "rectangle");
//        scriptBox.put(4, "rectangle");
//    }
//
//    @Test
//    public void test() {
//        Iterable<Scheduling> schedulings = schedulingService.findAll();
//        for (Scheduling scheduling : schedulings) {
//            List<String> scriptIds = new ArrayList<>();
//            List<JSONObject> nodes = new ArrayList<>();
//            List<JSONObject> lines = new ArrayList<>();
//            Map<String, Object> topology = new HashMap<>();
//            topology.put("nodes", nodes);
//            topology.put("lines", lines);
//            if (scheduling.getScriptId() == null) {
//                continue;
//            }
//            String scriptId = scheduling.getScriptId();
//            scriptIds.add(scriptId);
//            Script script = scriptService.findById(scriptId);
//            int x = 90;
//            int y = 50;
//            JSONObject node = getNode(x, y, script);
//            nodes.add(node);
//            if (StringUtils.isNotBlank(scheduling.getSubScriptIds())) {
//                JSONObject currentNode = new JSONObject(node);
//                String subScriptIds = scheduling.getSubScriptIds();
//                for (String subScriptId : subScriptIds.split(",")) {
//                    scriptIds.add(subScriptId);
//                    Script subScript =  scriptService.findById(subScriptId);
//                    x += 250;
//                    y = currentNode.getJSONObject("rect").getJSONObject("center").getIntValue("y") - scriptRectHeight.get(subScript.getType()) / 2;
//                    JSONObject subNode = getNode(x, y, subScript);
//                    nodes.add(subNode);
//                    JSONObject line = getLine(currentNode, subNode, subScript);
//                    lines.add(line);
//                    currentNode = new JSONObject(subNode);
//                }
//            }
//            scheduling.setScriptIds(StringUtils.join(scriptIds, ","));
//            scheduling.setTopology(JSON.toJSONString(topology));
//            schedulingService.save(scheduling);
//        }
//    }
//
//    private JSONObject getNode(int x, int y, Script script) {
//        int width = scriptRectWidth.get(script.getType());
//        int height = scriptRectHeight.get(script.getType());
//        JSONObject node = new JSONObject();
//        node.put("id", randomId());
//        node.put("text", script.getName());
//        Map<String, Object> rect = new HashMap<>();
//        rect.put("x", x);
//        rect.put("y", y);
//        rect.put("width", width);
//        rect.put("height", height);
//        JSONObject center = new JSONObject();
//        center.put("x", x + width / 2);
//        center.put("y", y + height / 2);
//        rect.put("center", center);
//        rect.put("ex", x + width);
//        rect.put("ey", y + height);
//        node.put("rect", rect);
//        node.put("paddingLeft", 10);
//        node.put("paddingRight", 10);
//        node.put("paddingTop", 10);
//        node.put("paddingBottom", 10);
//        node.put("borderRadius", 0.1);
//        node.put("name", scriptBox.get(script.getType()));
//        node.put("icon", scriptIconClass.get(script.getType()));
//        node.put("iconFamily", "iconfont");
//        node.put("iconColor", "#2f54eb");
//        node.put("textMaxLine", 1);
//        node.put("data", script.getId());
//        return node;
//    }
//
//    private JSONObject getLine(JSONObject currentNode, JSONObject subNode, Script script) {
//        JSONObject line = new JSONObject();
//        line.put("id", randomId());
//        JSONObject from = new JSONObject();
//        from.put("x", currentNode.getJSONObject("rect").getIntValue("ex"));
//        from.put("y", currentNode.getJSONObject("rect").getJSONObject("center").getIntValue("y"));
//        from.put("direction", 2);
//        from.put("anchorIndex", 2);
//        from.put("id", currentNode.getString("id"));
//        line.put("from", from);
//        line.put("fromArrow", "");
//        JSONObject to = new JSONObject();
//        to.put("x", subNode.getJSONObject("rect").getIntValue("x"));
//        to.put("y", subNode.getJSONObject("rect").getJSONObject("center").getIntValue("y"));
//        to.put("direction", 4);
//        to.put("anchorIndex", 0);
//        to.put("id", subNode.getString("id"));
//        line.put("to", to);
//        line.put("toArrow", "triangleSolid");
//        line.put("name", "curve");
//        JSONArray controlPoints = new JSONArray();
//        JSONObject controlPoint1 = new JSONObject();
//        controlPoint1.put("x", from.getIntValue("x") + 30);
//        controlPoint1.put("y", currentNode.getJSONObject("rect").getJSONObject("center").getIntValue("y"));
//        controlPoint1.put("id", currentNode.getString("id"));
//        controlPoints.add(controlPoint1);
//        JSONObject controlPoint2 = new JSONObject();
//        controlPoint2.put("x", to.getIntValue("x") - 30);
//        controlPoint2.put("y", subNode.getJSONObject("rect").getJSONObject("center").getIntValue("y"));
//        controlPoint2.put("id", subNode.getString("id"));
//        controlPoints.add(controlPoint2);
//        line.put("controlPoints", controlPoints);
//        return line;
//    }
//
//    private static String randomId() {
//        String letter = "0123456789abcdefghijklmnopqrstuvwxyz";
//        Random random = new Random();
//        StringBuilder builder = new StringBuilder();
//        for (int i = 0; i < 8; i ++) {
//            builder.append(letter.charAt(random.nextInt(letter.length())));
//        }
//        return builder.toString();
//    }
//}
