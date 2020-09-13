package com.meiyouframework.bigwhale.task.monitor;

import com.meiyouframework.bigwhale.common.Constant;
import com.meiyouframework.bigwhale.common.pojo.BackpressureInfo;
import com.meiyouframework.bigwhale.common.pojo.HttpYarnApp;
import com.meiyouframework.bigwhale.entity.YarnApp;
import com.meiyouframework.bigwhale.util.YarnApiUtils;
import org.quartz.DisallowConcurrentExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@DisallowConcurrentExecution
public class FlinkMonitorRunner extends AbstractMonitorRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlinkMonitorRunner.class);

    @Override
    public void executeJob() {
        YarnApp appInfo = getYarnAppFromDatabase();
        if (appInfo == null) {
            appInfo = getYarnAppFromYarnServer();
        }
        if (appInfo != null) {
            //判断是否存在running jobs
            boolean exist = YarnApiUtils.existRunningJobs(cluster.getYarnUrl(), appInfo.getAppId());
            if (!exist) {
                //五分钟
                if (System.currentTimeMillis() - appInfo.getStartedTime().getTime() >= 300000) {
                    YarnApiUtils.killApp(cluster.getYarnUrl(), appInfo.getAppId());
                    //防止更新不及时
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {

                    }
                    // 重启
                    boolean restart = restart();
                    if (restart) {
                        notice(null, scheduling, appInfo.getAppId(), Constant.ERROR_TYPE_FLINK_STREAMING_NO_RUNNING_JOB_RESTART);
                    } else {
                        notice(null, scheduling, appInfo.getAppId(), Constant.ERROR_TYPE_FLINK_STREAMING_NO_RUNNING_JOB_RESTART_FAILED);
                    }
                } else {
                    notice(null, scheduling, appInfo.getAppId(), Constant.ERROR_TYPE_FLINK_STREAMING_NO_RUNNING_JOB);
                }
                return;
            }
            //任务阻塞判断
            if (scheduling.getWaitingBatches() == 0) {
                return;
            }
            //获取背压数据
            BackpressureInfo backpressure = YarnApiUtils.backpressure(cluster.getYarnUrl(), appInfo.getAppId());
            if (backpressure == null) {
                return;
            }
            //阈值
            int maxBatches = scheduling.getWaitingBatches();
            boolean isOutBatches = backpressure.ratio >= maxBatches;
            //批次积压超阈值-告警
            if (isOutBatches) {
                LOGGER.info("flink task out of backpressure ratio, scriptId: " + scheduling.getScriptIds() + ", backpressure ratio(*100)="
                        + backpressure.ratio + ", max ratio(*100)=" + maxBatches);
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
                        notice(null, scheduling, appInfo.getAppId(), Constant.ERROR_TYPE_FLINK_STREAMING_BACKPRESSURE_RESTART + "(trouble vertex: " + backpressure.nextVertex + ")");
                    } else {
                        notice(null, scheduling, appInfo.getAppId(), Constant.ERROR_TYPE_FLINK_STREAMING_BACKPRESSURE_RESTART_FAILED + "(trouble vertex: " + backpressure.nextVertex + ")");
                    }
                } else {
                    notice(null, scheduling, appInfo.getAppId(), Constant.ERROR_TYPE_FLINK_STREAMING_BACKPRESSURE + "(trouble vertex: " + backpressure.nextVertex + ")");
                }
            }
        } else {
            HttpYarnApp httpYarnApp = getLastNoActiveYarnApp();
            if (scheduling.getExRestart() != null && scheduling.getExRestart()) {
                // 重启
                boolean restart = restart();
                if (restart) {
                    if (httpYarnApp != null) {
                        notice(null, scheduling, httpYarnApp.getId(), String.format(Constant.ERROR_TYPE_FLINK_STREAMING_UNUSUAL_RESTART, httpYarnApp.getFinalStatus()));
                    } else {
                        notice(null, scheduling, null, String.format(Constant.ERROR_TYPE_FLINK_STREAMING_UNUSUAL_RESTART, "UNKNOWN"));
                    }
                } else {
                    if (httpYarnApp != null) {
                        notice(null, scheduling, httpYarnApp.getId(), String.format(Constant.ERROR_TYPE_FLINK_STREAMING_UNUSUAL_RESTART_FAILED, httpYarnApp.getFinalStatus()));
                    } else {
                        notice(null, scheduling, null, String.format(Constant.ERROR_TYPE_FLINK_STREAMING_UNUSUAL_RESTART_FAILED, "UNKNOWN"));
                    }
                }
            } else {
                if (httpYarnApp != null) {
                    notice(null, scheduling, httpYarnApp.getId(), String.format(Constant.ERROR_TYPE_FLINK_STREAMING_UNUSUAL, httpYarnApp.getFinalStatus()));
                } else {
                    notice(null, scheduling, null, String.format(Constant.ERROR_TYPE_FLINK_STREAMING_UNUSUAL, "UNKNOWN"));
                }
            }
        }
    }

}
