package com.meiyou.bigwhale.scheduler.system;

import com.meiyou.bigwhale.common.Constant;
import com.meiyou.bigwhale.common.pojo.HttpYarnApp;
import com.meiyou.bigwhale.config.YarnConfig;
import com.meiyou.bigwhale.entity.YarnApp;
import com.meiyou.bigwhale.service.ClusterService;
import com.meiyou.bigwhale.service.ClusterUserService;
import com.meiyou.bigwhale.service.YarnAppService;
import com.meiyou.bigwhale.scheduler.AbstractNoticeable;
import com.meiyou.bigwhale.util.YarnApiUtils;
import com.meiyou.bigwhale.entity.Cluster;
import com.meiyou.bigwhale.entity.ClusterUser;
import org.quartz.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;


/**
 * @author Suxy
 * @date 2019/8/29
 * @description file description
 */
@DisallowConcurrentExecution
public class ActiveYarnAppRefresher extends AbstractNoticeable implements InterruptableJob {

    private static int checkAppDuplicateAndNoRunningSkipCount = 0;
    private static int checkAppMemorySkipCount = 0;
    private Thread thread;
    private volatile boolean interrupted = false;

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private YarnAppService yarnAppService;
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
        checkAppDuplicateAndNoRunningSkipCount ++;
        checkAppMemorySkipCount ++;
        // 遍历集群
        Iterable<Cluster> clusters = clusterService.findAll();
        for (Cluster cluster : clusters) {
            Integer clusterId = cluster.getId();
            // 请求yarn web url, 获取所有应用
            List<HttpYarnApp> httpYarnApps = YarnApiUtils.getActiveApps(cluster.getYarnUrl());
            // 请求出错，不清理数据
            if (httpYarnApps == null) {
                continue;
            }
            if (httpYarnApps.isEmpty()) {
                //清理数据
                yarnAppService.deleteByQuery("clusterId=" + clusterId);
                continue;
            }
            List<YarnApp> yarnApps = new ArrayList<>();
            for (HttpYarnApp httpYarnApp : httpYarnApps) {
                YarnApp yarnApp = new YarnApp();
                BeanUtils.copyProperties(httpYarnApp, yarnApp);
                yarnApp.setId(null);
                yarnApp.setClusterId(clusterId);
                String queue = httpYarnApp.getQueue();
                // 以队列为准
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
                            yarnApp.setUserId(clusterUser.getUserId());
                            match = true;
                            break;
                        }
                    }
                    if (!match) {
                        yarnApp.setUserId(clusterUsers.get(0).getUserId());
                    }
                }
                yarnApp.setAppId(httpYarnApp.getId());
                yarnApp.setStartedTime(new Date(httpYarnApp.getStartedTime()));
                yarnApp.setRefreshTime(new Date());
                yarnApps.add(yarnApp);
            }
            // 先标记再删除
            List<YarnApp> oldYarnApps = yarnAppService.findByQuery("clusterId=" + clusterId);
            if (yarnApps.size() > 0) {
                yarnAppService.saveAll(yarnApps);
            }
            if (!CollectionUtils.isEmpty(oldYarnApps)) {
                List<Integer> ids = new ArrayList<>(oldYarnApps.size());
                oldYarnApps.forEach(yarnApp -> ids.add(yarnApp.getId()));
                yarnAppService.deleteByQuery("id=" + StringUtils.collectionToDelimitedString(ids, ","));
            }
            if (checkAppDuplicateAndNoRunningSkipCount >= 30) {
                checkAppDuplicate(cluster, yarnApps);
                checkAppNoRunning(cluster, yarnApps);
            }
            if (checkAppMemorySkipCount >= 180) {
                checkAppMemory(cluster, yarnApps);
            }
        }
        if (checkAppDuplicateAndNoRunningSkipCount >= 30) {
            checkAppDuplicateAndNoRunningSkipCount = 0;
        }
        if (checkAppMemorySkipCount >= 180) {
            checkAppMemorySkipCount = 0;
        }
    }

    /**
     * 检查重复应用
     * @param cluster
     * @param yarnApps
     */
    private void checkAppDuplicate(Cluster cluster, List<YarnApp> yarnApps) {
        Map<String, List<YarnApp>> appInstanceMap = new HashMap<>();
        for (YarnApp yarnApp : yarnApps) {
            String key;
            if (yarnApp.getName().contains(".bw_instance_s")) {
                key = yarnApp.getUser() + ";" + yarnApp.getQueue() + ";" + yarnApp.getName().split("\\.bw_instance_")[0];
            } else {
                key = yarnApp.getUser() + ";" + yarnApp.getQueue() + ";" + yarnApp.getName();
            }
            List<YarnApp> vals = appInstanceMap.computeIfAbsent(key, k -> new ArrayList<>());
            vals.add(yarnApp);
        }
        for (Map.Entry<String, List<YarnApp>> entry : appInstanceMap.entrySet()) {
            List<YarnApp> vals = entry.getValue();
            if (vals.size() > 1) {
                YarnApp yarnApp = vals.get(0);
                StringBuilder trackingUrl = new StringBuilder();
                vals.forEach(item -> trackingUrl.append(item.getTrackingUrl()).append(", "));
                String trackingUrls = trackingUrl.substring(0, trackingUrl.length() - 2);
                notice(cluster, yarnApp, trackingUrls, Constant.ErrorType.APP_DUPLICATE);
            }
        }
    }

    private void checkAppNoRunning(Cluster cluster, List<YarnApp> yarnApps) {
        long nowTimestamp = System.currentTimeMillis();
        for (YarnApp yarnApp : yarnApps) {
            if (Constant.JobState.ACCEPTED.equals(yarnApp.getState()) && nowTimestamp - yarnApp.getStartedTime().getTime() >= 600000) {
                String trackingUrl = yarnApp.getTrackingUrl();
                notice(cluster, yarnApp, trackingUrl, Constant.ErrorType.APP_NO_RUNNING);
            }
        }
    }

    /**
     * 大内存应用检查
     * @param cluster
     * @param yarnApps
     */
    private void checkAppMemory(Cluster cluster, List<YarnApp> yarnApps) {
        if (yarnConfig.getAppMemoryThreshold() <= 0) {
            return;
        }
        for (YarnApp yarnApp : yarnApps) {
            if ("RUNNING".equalsIgnoreCase(yarnApp.getState()) && yarnApp.getAllocatedMB() != null) {
                if (yarnApp.getAllocatedMB() >= yarnConfig.getAppMemoryThreshold() && !yarnConfig.getAppWhiteList().contains(yarnApp.getName().split("\\.bw_instance_")[0])) {
                    String trackingUrl = yarnApp.getTrackingUrl();
                    notice(cluster, yarnApp, trackingUrl, Constant.ErrorType.APP_MEMORY_OVERLIMIT);
                }
            }
        }
    }

}
