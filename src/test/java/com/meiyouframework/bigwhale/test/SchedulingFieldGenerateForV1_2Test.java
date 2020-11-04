//package com.meiyouframework.bigwhale.test;
//
//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.JSONArray;
//import com.alibaba.fastjson.JSONObject;
//import com.meiyouframework.bigwhale.entity.Scheduling;
//import com.meiyouframework.bigwhale.service.SchedulingService;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.junit4.SpringRunner;
//
///**
// * @author Suxy
// * @date 2020/11/04
// * @description v1.1离线调度更新到v1.2
// * 1. 全选，打开文件注释
// * 2. 备份scheduling表（防止更新过程出现错误）
// * 3. 修改相应的@ActiveProfiles
// * 4. 确认相应的配置文件中数据库的配置信息
// * 5. 运行此测试
// * 6. 升级完毕
// */
//@SpringBootTest
//@RunWith(SpringRunner.class)
//@ActiveProfiles("dev")
//public class SchedulingFieldGenerateForV1_2Test {
//
//    @Autowired
//    private SchedulingService schedulingService;
//
//    @Test
//    public void test() {
//        Iterable<Scheduling> schedulings = schedulingService.findAll();
//        for (Scheduling scheduling : schedulings) {
//            if (scheduling.getTopology() == null) {
//                continue;
//            }
//            JSONObject jsonObject = JSON.parseObject(scheduling.getTopology());
//            JSONArray nodes = jsonObject.getJSONArray("nodes");
//            for (Object node : nodes) {
//                JSONObject nodeObj = (JSONObject) node;
//                String scriptId = nodeObj.getString("data");
//                Scheduling.NodeData data = new Scheduling.NodeData(scriptId, 0 , 1);
//                nodeObj.put("data", data);
//                scheduling.setTopology(JSON.toJSONString(jsonObject));
//                schedulingService.save(scheduling);
//            }
//        }
//    }
//
//}
