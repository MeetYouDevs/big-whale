package com.meiyou.bigwhale.task.common;

import com.meiyou.bigwhale.common.Constant;
import com.meiyou.bigwhale.common.pojo.HttpYarnApp;
import com.meiyou.bigwhale.config.YarnConfig;
import com.meiyou.bigwhale.entity.Script;
import com.meiyou.bigwhale.entity.YarnApp;
import com.meiyou.bigwhale.service.ClusterService;
import com.meiyou.bigwhale.service.ClusterUserService;
import com.meiyou.bigwhale.service.ScriptService;
import com.meiyou.bigwhale.service.YarnAppService;
import com.meiyou.bigwhale.task.AbstractNoticeableTask;
import com.meiyou.bigwhale.util.YarnApiUtils;
import com.meiyou.bigwhale.entity.Cluster;
import com.meiyou.bigwhale.entity.ClusterUser;
import org.quartz.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Pattern;


/**
 * @author Suxy
 * @date 2019/8/29
 * @description file description
 */
@DisallowConcurrentExecution
public class RefreshActiveStateAppsJob extends AbstractNoticeableTask implements InterruptableJob {

    private static final Pattern PATTERN = Pattern.compile("_instance\\d+$");

    private static int checkAppDuplicateSkipCount = 0;
    private static int checkAppMemorySkipCount = 0;
    private Thread thread;
    private volatile boolean interrupted = false;

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private YarnAppService yarnAppService;
    @Autowired
    private ScriptService scriptService;
    @Autowired
    private ClusterUserService clusterUserService;
    @Autowired
    private YarnConfig yarnConfig;

    @Override
    public void interrupt() {
        if (!interrupted) {
            interrupted = true;
            thread.interrupt();
        }
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        thread = Thread.currentThread();
        checkAppDuplicateSkipCount ++;
        checkAppMemorySkipCount ++;
        Date now = new Date();
        Iterable<Cluster> clusters = clusterService.findAll();
        // 遍历集群
        for (Cluster cluster : clusters) {
            String clusterId = cluster.getId();
            // 请求yarn web url, 获取活跃应用
            List<HttpYarnApp> apps = YarnApiUtils.getActiveStateApps(cluster.getYarnUrl());
            //请求出错，不清理数据
            if (apps == null) {
                continue;
            }
            if (apps.isEmpty()) {
                //清理数据
                yarnAppService.deleteByQuery("clusterId=" + clusterId);
                continue;
            }
            Map<String, Script> scriptInfoMap = scriptService.getAppMap(clusterId);
            List<YarnApp> appInfos = new ArrayList<>();
            for (HttpYarnApp app : apps) {
                Script script;
                if (PATTERN.matcher(app.getName()).find()) {
                    script = scriptInfoMap.get(app.getUser() + Constant.APP_APPEND_SYMBOL + app.getQueue() + Constant.APP_APPEND_SYMBOL + app.getName().split("_instance")[0]);
                } else {
                    script = scriptInfoMap.get(app.getUser() + Constant.APP_APPEND_SYMBOL + app.getQueue() + Constant.APP_APPEND_SYMBOL + app.getName());
                }
                YarnApp yarnApp = new YarnApp();
                BeanUtils.copyProperties(app, yarnApp);
                yarnApp.setId(null);
                if (script != null) {
                    yarnApp.setUid(script.getUid());
                    yarnApp.setScriptId(script.getId());
                } else {
                    String queue = app.getQueue();
                    List<ClusterUser> clusterUsers = clusterUserService.findByClusterIdAndQueue(clusterId, queue);
                    if (clusterUsers.isEmpty()) {
                        if (queue.startsWith("root.")) {
                            queue = queue.substring(5);
                            clusterUsers = clusterUserService.findByClusterIdAndQueue(clusterId, queue);
                        }
                    }
                    if (!clusterUsers.isEmpty()) {
                        boolean match = false;
                        for (ClusterUser clusterUser : clusterUsers) {
                            if (yarnApp.getUser().equals(clusterUser.getUser())) {
                                yarnApp.setUid(clusterUser.getUid());
                                match = true;
                                break;
                            }
                        }
                        if (!match) {
                            yarnApp.setUid(clusterUsers.get(0).getUid());
                        }
                    }
                }
                yarnApp.setClusterId(clusterId);
                yarnApp.setUpdateTime(now);
                yarnApp.setAppId(app.getId());
                yarnApp.setStartedTime(new Date(app.getStartedTime()));
                appInfos.add(yarnApp);
            }
            //先标记再删除
            List<YarnApp> yarnApps = yarnAppService.findByQuery("clusterId=" + clusterId);
            if (appInfos.size() > 0) {
                yarnAppService.saveAll(appInfos);
            }
            if (!CollectionUtils.isEmpty(yarnApps)) {
                List<String> ids = new ArrayList<>(yarnApps.size());
                yarnApps.forEach(item -> ids.add(item.getId()));
                yarnAppService.deleteByQuery("id=" + StringUtils.collectionToDelimitedString(ids, ","));
            }
            if (checkAppDuplicateSkipCount >= 30) {
                checkAppDuplicate(cluster, apps, scriptInfoMap);
            }
            if (checkAppMemorySkipCount >= 300) {
                checkAppMemory(cluster, apps, scriptInfoMap);
            }
        }
        if (checkAppDuplicateSkipCount >= 30) {
            checkAppDuplicateSkipCount = 0;
        }
        if (checkAppMemorySkipCount >= 300) {
            checkAppMemorySkipCount = 0;
        }
    }

    /**
     * 检查重复应用
     * @param cluster
     * @param apps
     * @param scriptInfoMap
     */
    private void checkAppDuplicate(Cluster cluster, List<HttpYarnApp> apps, Map<String, Script> scriptInfoMap) {
        Map<String, List<HttpYarnApp>> appInstanceMap = new HashMap<>();
        for (HttpYarnApp app : apps) {
            String key = app.getUser() + Constant.APP_APPEND_SYMBOL + app.getQueue() + Constant.APP_APPEND_SYMBOL + app.getName();
            if (appInstanceMap.containsKey(key)) {
                appInstanceMap.get(key).add(app);
            } else {
                List<HttpYarnApp> httpYarnApps = new ArrayList<>();
                httpYarnApps.add(app);
                appInstanceMap.put(key, httpYarnApps);
            }
        }
        for (Map.Entry<String, List<HttpYarnApp>> stringListEntry : appInstanceMap.entrySet()) {
            String key = stringListEntry.getKey();
            List<HttpYarnApp> vals = stringListEntry.getValue();
            if (vals.size() > 1) {
                Script script = scriptInfoMap.get(key);
                HttpYarnApp httpYarnApp = vals.get(0);
                StringBuilder trackingUrl = new StringBuilder();
                vals.forEach(item -> trackingUrl.append(item.getTrackingUrl()).append(", "));
                String trackingUrls = trackingUrl.substring(0, trackingUrl.length() - 2);
                notice(cluster, script, httpYarnApp, trackingUrls, Constant.ERROR_TYPE_APP_DUPLICATE);
            }
        }
    }

    /**
     * 大内存应用检查
     * @param cluster
     * @param apps
     * @param scriptInfoMap
     */
    private void checkAppMemory(Cluster cluster, List<HttpYarnApp> apps, Map<String, Script> scriptInfoMap) {
        if (yarnConfig.getAppMemoryThreshold() <= 0) {
            return;
        }
        for (HttpYarnApp httpYarnApp : apps) {
            if ("RUNNING".equalsIgnoreCase(httpYarnApp.getState()) && httpYarnApp.getAllocatedMB() != null) {
                if (httpYarnApp.getAllocatedMB() >= yarnConfig.getAppMemoryThreshold() && !yarnConfig.getAppWhiteList().contains(httpYarnApp.getName())) {
                    String key = httpYarnApp.getUser() + Constant.APP_APPEND_SYMBOL + httpYarnApp.getQueue() + Constant.APP_APPEND_SYMBOL + httpYarnApp.getName();
                    Script script = scriptInfoMap.get(key);
                    String trackingUrl = httpYarnApp.getTrackingUrl();
                    notice(cluster, script, httpYarnApp, trackingUrl, Constant.ERROR_TYPE_APP_MEMORY_OVERLIMIT);
                }
            }
        }
    }

}
