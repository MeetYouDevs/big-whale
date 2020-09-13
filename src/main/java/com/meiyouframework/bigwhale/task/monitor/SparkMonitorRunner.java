package com.meiyouframework.bigwhale.task.monitor;

import com.meiyouframework.bigwhale.common.Constant;
import com.meiyouframework.bigwhale.common.pojo.HttpYarnApp;
import com.meiyouframework.bigwhale.entity.YarnApp;
import com.meiyouframework.bigwhale.util.YarnApiUtils;
import org.quartz.DisallowConcurrentExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author meiyou big data group
 * @date 2020/01/08
 */
@DisallowConcurrentExecution
public class SparkMonitorRunner extends AbstractMonitorRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SparkMonitorRunner.class);

    @Override
    public void executeJob() {
        YarnApp appInfo = getYarnAppFromDatabase();
        if (appInfo == null) {
            appInfo = getYarnAppFromYarnServer();
        }
        if (appInfo != null) {
            //批次积压判断
            if (scheduling.getWaitingBatches() == 0) {
                return;
            }
            //获取当前正在运行批次数
            int waitingBatches = YarnApiUtils.waitingBatches(cluster.getYarnUrl(), appInfo.getAppId());
            //阈值
            int maxBatches = scheduling.getWaitingBatches();
            boolean isOutBatches = waitingBatches >= maxBatches;
            //批次积压超阈值-告警
            if (isOutBatches) {
                LOGGER.info("spark task out of batches, scriptId: " + scheduling.getScriptIds() + ", waitingBatches="
                        + waitingBatches + ", maxBatches=" + maxBatches);
                //积压重启
                if (scheduling.getBlockingRestart()) {
                    YarnApiUtils.killApp(cluster.getYarnUrl(), appInfo.getAppId());
                    //防止更新不及时
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {

                    }
                    // 重启
                    boolean restart = restart();
                    if (restart) {
                        notice(null, scheduling, appInfo.getAppId(), Constant.ERROR_TYPE_SPARK_STREAMING_WAITING_BATCH_RESTART);
                    } else {
                        notice(null, scheduling, appInfo.getAppId(), Constant.ERROR_TYPE_SPARK_STREAMING_WAITING_BATCH_RESTART_FAILED);
                    }
                } else {
                    notice(null, scheduling, appInfo.getAppId(), Constant.ERROR_TYPE_SPARK_STREAMING_WAITING_BATCH);
                }
            }
        } else {
            HttpYarnApp httpYarnApp = getLastNoActiveYarnApp();
            if (scheduling.getExRestart()) {
                //重启
                boolean restart = restart();
                if (restart) {
                    if (httpYarnApp != null) {
                        notice(null, scheduling, httpYarnApp.getId(), String.format(Constant.ERROR_TYPE_SPARK_STREAMING_UNUSUAL_RESTART, httpYarnApp.getFinalStatus()));
                    } else {
                        notice(null, scheduling, null, String.format(Constant.ERROR_TYPE_SPARK_STREAMING_UNUSUAL_RESTART, "UNKNOWN"));
                    }
                } else {
                    if (httpYarnApp != null) {
                        notice(null, scheduling, httpYarnApp.getId(), String.format(Constant.ERROR_TYPE_SPARK_STREAMING_UNUSUAL_RESTART_FAILED, httpYarnApp.getFinalStatus()));
                    } else {
                        notice(null, scheduling, null, String.format(Constant.ERROR_TYPE_SPARK_STREAMING_UNUSUAL_RESTART_FAILED, "UNKNOWN"));
                    }
                }
            } else {
                if (httpYarnApp != null) {
                    notice(null, scheduling, httpYarnApp.getId(), String.format(Constant.ERROR_TYPE_SPARK_STREAMING_UNUSUAL, httpYarnApp.getFinalStatus()));
                } else {
                    notice(null, scheduling, null, String.format(Constant.ERROR_TYPE_SPARK_STREAMING_UNUSUAL, "UNKNOWN"));
                }

            }
        }
    }
}
