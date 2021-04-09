package com.meiyou.bigwhale.util;

import com.alibaba.fastjson.*;
import com.meiyou.bigwhale.common.pojo.BackpressureInfo;
import com.meiyou.bigwhale.common.pojo.HttpYarnApp;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class YarnApiUtils {

    private static final Logger LOG = LoggerFactory.getLogger(YarnApiUtils.class);
    public static final String HTTP_URL_REGX = ".*http://(.*:\\d+)/.*";

    private static final Map<String, String> HEADERS;

    static {
        HEADERS = new HashMap<>();
        HEADERS.put("Content-Type", "application/json");
        HEADERS.put("Accept", "application/json; charset=UTF-8");
    }

    private YarnApiUtils() {

    }

    public static List<HttpYarnApp> getActiveApps(String yarnUrl) {
        Map<String, Object> params = new HashMap<>();
        params.put("states", "new,new_saving,submitted,accepted,running");
        OkHttpUtils.Result result = OkHttpUtils.doGet(getAppsUrl(yarnUrl), params, HEADERS);
        if (result.isSuccessful && StringUtils.isNotEmpty(result.content)) {
            return parseAppsApiResponse(result);
        }
        return null;
    }

    /**
     * 获取运行中的应用
     * @param yarnUrl
     * @param user
     * @param queue
     * @param name
     * @param retries
     * @return
     */
    public static HttpYarnApp getActiveApp(String yarnUrl, String user, String queue, String name, int retries) {
        if (queue != null && !"root".equals(queue) && !queue.startsWith("root.")) {
            queue = "root." + queue;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("user", user);
        params.put("queue", queue);
        params.put("states", "new,new_saving,submitted,accepted,running");
        for (;;) {
            OkHttpUtils.Result result = OkHttpUtils.doGet(getAppsUrl(yarnUrl), params, HEADERS);
            List<HttpYarnApp> appList = parseAppsApiResponse(result);
            if (!appList.isEmpty()) {
                appList.sort((app1, app2) -> {
                    long time1 = app1.getStartedTime();
                    long time2 = app2.getStartedTime();
                    return Long.compare(time2, time1);
                });
                for (HttpYarnApp httpYarnApp : appList) {
                    if (httpYarnApp.getName().equals(name)) {
                        return httpYarnApp;
                    }
                }
            }
            if (retries <= 0) {
                break;
            }
            retries --;
        }
        return null;
    }

    /**
     * 获取最后一次提交的未运行的应用的状态
     * @param yarnUrl rm url
     * @param user 用户
     * @param name 应用名称
     * @param retries 重试次数
     * @return
     */
    public static HttpYarnApp getLastNoActiveApp(String yarnUrl, String user, String queue, String name, int retries) {
        if (queue != null && !"root".equals(queue) && !queue.startsWith("root.")) {
            queue = "root." + queue;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("user", user);
        params.put("states", "finished,killed,failed");
        for (;;) {
            OkHttpUtils.Result result = OkHttpUtils.doGet(getAppsUrl(yarnUrl), params, HEADERS);
            List<HttpYarnApp> appList = parseAppsApiResponse(result);
            if (!appList.isEmpty()) {
                appList.sort((app1, app2) -> {
                    long time1 = app1.getFinishedTime();
                    long time2 = app2.getFinishedTime();
                    return Long.compare(time2, time1);
                });
                for (HttpYarnApp httpYarnApp : appList) {
                    if (httpYarnApp.getQueue().equals(queue) && httpYarnApp.getName().equals(name)) {
                        return httpYarnApp;
                    }
                }
            }
            if (retries <= 0) {
                break;
            }
            retries --;
        }
        return null;
    }

    /**
     * 通过ID获取应用信息
     * @param yarnUrl
     * @param appId
     * @return
     */
    public static HttpYarnApp getApp(String yarnUrl, String appId) {
        String url = getAppsUrl(yarnUrl) + "/" + appId;
        OkHttpUtils.Result result = OkHttpUtils.doGet(url, null, HEADERS);
        if (result.isSuccessful && StringUtils.isNotEmpty(result.content)) {
            JSONObject jsonObject = JSON.parseObject(result.content);
            return JSON.parseObject(jsonObject.getString("app"), HttpYarnApp.class);
        }
        return null;
    }

    /**
     * spark 批次
     * @param yarnUrl
     * @param appId
     * @return
     */
    public static int waitingBatches(String yarnUrl, String appId) {
        String url = appendUrl(yarnUrl) + "proxy/%s/metrics/json";
        url = String.format(url, appId);
        OkHttpUtils.Result result = OkHttpUtils.doGet(url, null, HEADERS);
        if (result.isSuccessful && StringUtils.isNotEmpty(result.content)) {
            try {
                JSONObject metrics = JSON.parseObject(result.content).getJSONObject("gauges");
                for (String key : metrics.keySet()) {
                    if (key.endsWith("streaming.waitingBatches")) {
                        return metrics.getJSONObject(key).getIntValue("value");
                    }
                }
            } catch (JSONException e) {
                //未处于运行状态的APP会返回html信息的问题
            }
        }
        return -1;
    }

    /**
     * flink 判断是否存在运行中的job
     * @param yarnUrl
     * @param appId
     * @return
     */
    public static boolean existRunningJobs(String yarnUrl, String appId) {
        //获取jobId
        String url = appendUrl(yarnUrl) + "proxy/%s/jobs";
        url = String.format(url, appId);
        OkHttpUtils.Result result = OkHttpUtils.doGet(url, null, HEADERS);
        if (result.isSuccessful && StringUtils.isNotEmpty(result.content)) {
            try {
                JSONArray jobs = JSON.parseObject(result.content).getJSONArray("jobs");
                if (jobs != null) {
                    for (int i = 0; i < jobs.size(); i ++) {
                        JSONObject job = jobs.getJSONObject(i);
                        if ("RUNNING".equals(job.get("status"))) {
                            return true;
                        }
                    }
                }
                //for 1.4 version
                jobs = JSON.parseObject(result.content).getJSONArray("jobs-running");
                return jobs != null && jobs.size() > 0;
            } catch (JSONException e) {
                //未处于运行状态的APP会返回html信息的问题
            }
        }
        //请求失败判定为存在
        return true;
    }

    /**
     * flink 背压监测阻塞任务数
     * @param yarnUrl
     * @param appId
     * @return
     */
    public static BackpressureInfo backpressure(String yarnUrl, String appId) {
        //获取jobId
        String url = appendUrl(yarnUrl) + "proxy/%s/jobs";
        url = String.format(url, appId);
        OkHttpUtils.Result result = OkHttpUtils.doGet(url, null, HEADERS);
        if (result.isSuccessful && StringUtils.isNotEmpty(result.content)) {
            try {
                //获得job id列表，一般只有一个
                String id = null;
                JSONArray jobs = JSON.parseObject(result.content).getJSONArray("jobs");
                if (jobs != null) {
                    JSONObject job = jobs.getJSONObject(0);
                    if (job != null && "RUNNING".equals(job.get("status"))) {
                        id = job.getString("id");
                    }
                }
                if (id == null) {
                    jobs = JSON.parseObject(result.content).getJSONArray("jobs-running");
                    if (jobs != null && jobs.size() > 0) {
                        id = jobs.getString(0);
                    }
                }
                if (id != null) {
                    //获取顶点列表
                    url += "/" + id;
                    result = OkHttpUtils.doGet(url, null, HEADERS);
                    if (result.isSuccessful && StringUtils.isNotEmpty(result.content)) {
                        JSONObject jobDetails = JSON.parseObject(result.content);
                        JSONArray vertices = jobDetails.getJSONArray("vertices");
                        for (int i = vertices.size() - 1; i >= 0; i--) {
                            //获取顶点背压监控数据
                            JSONObject vertexObject = vertices.getJSONObject(i);
                            String vertexId = vertexObject.getString("id");
                            String backpressureUrl = url + "/vertices/" + vertexId + "/backpressure";
                            JSONArray subtasks = null;
                            int count = 0;
                            for (; ; ) {
                                count ++;
                                //背压数据只有请求的时候才收集，所以第一次一般无数据
                                result = OkHttpUtils.doGet(backpressureUrl, null, HEADERS);
                                if (result.isSuccessful && StringUtils.isNotEmpty(result.content)) {
                                    subtasks = JSON.parseObject(result.content).getJSONArray("subtasks");
                                    if (subtasks != null) {
                                        break;
                                    }
                                    Thread.sleep(2000);
                                }
                                //重试十次无结果就返回
                                if (count > 10) {
                                    break;
                                }
                            }
                            if (subtasks != null) {
                                for (int j = 0; j < subtasks.size(); j++) {
                                    JSONObject subtask = subtasks.getJSONObject(j);
                                    //大于0
                                    if (subtask.getDouble("ratio") > 0) {
                                        Set<String> names = new HashSet<>();
                                        JSONArray nodes = jobDetails.getJSONObject("plan").getJSONArray("nodes");
                                        for (int k = 0; k < nodes.size(); k ++) {
                                            JSONObject node = nodes.getJSONObject(k);
                                            JSONArray jsonArray = node.getJSONArray("inputs");
                                            if (jsonArray != null) {
                                                jsonArray.forEach(item -> {
                                                    if (vertexId.equals(((JSONObject)item).getString("id"))) {
                                                        names.add(node.getString("description").replaceAll("-&gt", "->"));
                                                    }
                                                });
                                            }
                                        }
                                        if (!names.isEmpty()) {
                                            return new BackpressureInfo((int) (subtask.getDouble("ratio") * 100), StringUtils.join(names, ","));
                                        } else {
                                            return new BackpressureInfo((int) (subtask.getDouble("ratio") * 100), vertexObject.getString("name"));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (JSONException e) {
                //未处于运行状态的APP会返回html信息的问题
            } catch (Exception e) {
                LOG.error("backpressure execute error: " + e.getMessage(), e);
            }
        }
        return null;
    }

    public static boolean killApp(String yarnUrl, String appId) {
        String stateUrl = getAppsUrl(yarnUrl) + "/" + appId + "/state";
        OkHttpUtils.Result result = OkHttpUtils.doPut(stateUrl, OkHttpUtils.MEDIA_JSON, "{\"state\": \"KILLED\"}", HEADERS);
        if (result.isSuccessful && StringUtils.isNotEmpty(result.content)) {
            JSONObject jsonObject = JSON.parseObject(result.content);
            String state = jsonObject.getString("state");
            return StringUtils.isNotEmpty(state);
        }
        return false;
    }

    private static String getAppsUrl(String yarnUrl) {
        return appendUrl(yarnUrl) + "ws/v1/cluster/apps";
    }

    private static String appendUrl(String url) {
        if (!url.endsWith("/")) {
            url += "/";
        }
        return url;
    }

    private static List<HttpYarnApp> parseAppsApiResponse(OkHttpUtils.Result result) {
        if (result.isSuccessful && StringUtils.isNotEmpty(result.content)) {
            JSONObject jsonObject = JSON.parseObject(result.content);
            if (jsonObject != null) {
                JSONObject apps = jsonObject.getJSONObject("apps");
                if (apps != null) {
                    String app = apps.getString("app");
                    if (StringUtils.isNotEmpty(app)) {
                        return JSON.parseArray(app, HttpYarnApp.class);
                    }
                }
            }
        }
        return new ArrayList<>();
    }

}
