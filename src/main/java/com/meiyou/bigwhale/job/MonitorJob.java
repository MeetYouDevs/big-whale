package com.meiyou.bigwhale.job;

import com.meiyou.bigwhale.common.Constant;
import com.meiyou.bigwhale.common.pojo.BackpressureInfo;
import com.meiyou.bigwhale.entity.Cluster;
import com.meiyou.bigwhale.entity.Monitor;
import com.meiyou.bigwhale.entity.Script;
import com.meiyou.bigwhale.entity.ScriptHistory;
import com.meiyou.bigwhale.service.*;
import com.meiyou.bigwhale.util.SchedulerUtils;
import com.meiyou.bigwhale.util.YarnApiUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Suxy
 * @date 2019/8/29
 * @description file description
 */
@DisallowConcurrentExecution
public class MonitorJob extends AbstractNoticeableJob implements InterruptableJob {

    private Thread thread;
    private volatile boolean interrupted = false;

    private Monitor monitor;
    private ScriptHistory scriptHistory;
    private Script script;
    private Cluster cluster;

    @Autowired
    private MonitorService monitorService;
    @Autowired
    private ScriptHistoryService scriptHistoryService;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private ScriptService scriptService;

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
        Integer monitorId = Integer.parseInt(jobExecutionContext.getJobDetail().getKey().getName());
        monitor = monitorService.findById(monitorId);
        monitor.setRealFireTime(jobExecutionContext.getFireTime());
        monitorService.save(monitor);
        script = scriptService.findOneByQuery("monitorId=" + monitorId);
        scriptHistory = scriptHistoryService.findNoScheduleLatestByScriptId(script.getId());
        if (scriptHistory == null) {
            restart();
            return;
        }
        // 手动启动的任务，后面才开启的监控，设置一下监控ID
        if (scriptHistory.getMonitorId() == null) {
            scriptHistory.setMonitorId(monitorId);
            scriptHistoryService.save(scriptHistory);
        }
        if (Constant.JobState.INITED.equals(scriptHistory.getState()) ||
                Constant.JobState.SUBMITTING.equals(scriptHistory.getState())) {
            return;
        }
        if (scriptHistory.isRunning() && scriptHistory.getJobId() == null) {
            return;
        }
        cluster = clusterService.findById(script.getClusterId());
        if (Constant.ScriptType.SPARK_STREAM.equals(scriptHistory.getScriptType())) {
            monitorSparkStream();
        } else if (Constant.ScriptType.FLINK_STREAM.equals(scriptHistory.getScriptType())) {
            monitorFlinkStream();
        }
    }

    private void monitorSparkStream() {
        if (scriptHistory.isRunning()) {
            // 批次积压判断
            if (monitor.getWaitingBatches() == 0) {
                return;
            }
            // 获取当前正在运行批次数
            int waitingBatches = YarnApiUtils.waitingBatches(cluster.getYarnUrl(), scriptHistory.getJobId());
            // 阈值
            int maxBatches = monitor.getWaitingBatches();
            boolean isOutBatches = waitingBatches >= maxBatches;
            // 批次积压超阈值-告警
            if (isOutBatches) {
                // 积压重启
                if (monitor.getBlockingRestart()) {
                    YarnApiUtils.killApp(cluster.getYarnUrl(), scriptHistory.getJobId());
                    scriptHistory.updateState(Constant.JobState.KILLED);
                    scriptHistoryService.save(scriptHistory);
                    boolean restart = restart();
                    if (restart) {
                        notice(scriptHistory, Constant.ErrorType.SPARK_STREAM_WAITING_BATCH_RESTART);
                    } else {
                        notice(scriptHistory, Constant.ErrorType.SPARK_STREAM_WAITING_BATCH_RESTART_FAILED);
                    }
                } else {
                    notice(scriptHistory, Constant.ErrorType.SPARK_STREAM_WAITING_BATCH);
                }
            }
        } else {
            // 异常重启
            if (monitor.getExRestart()) {
                boolean restart = restart();
                if (restart) {
                    notice(scriptHistory, String.format(Constant.ErrorType.SPARK_STREAM_UNUSUAL_RESTART, scriptHistory.getJobFinalStatus()));
                } else {
                    notice(scriptHistory, String.format(Constant.ErrorType.SPARK_STREAM_UNUSUAL_RESTART_FAILED, scriptHistory.getJobFinalStatus()));
                }
            } else {
                notice(scriptHistory, String.format(Constant.ErrorType.SPARK_STREAM_UNUSUAL, scriptHistory.getJobFinalStatus()));
            }
        }
    }

    private void monitorFlinkStream() {
        if (scriptHistory.isRunning()) {
            boolean exist = YarnApiUtils.existRunningJobs(cluster.getYarnUrl(), scriptHistory.getJobId());
            if (!exist) {
                // 五分钟
                if (System.currentTimeMillis() - scriptHistory.getStartTime().getTime() >= 300000) {
                    YarnApiUtils.killApp(cluster.getYarnUrl(), scriptHistory.getJobId());
                    scriptHistory.updateState(Constant.JobState.KILLED);
                    scriptHistoryService.save(scriptHistory);
                    boolean restart = restart();
                    if (restart) {
                        notice(scriptHistory, Constant.ErrorType.FLINK_STREAM_NO_RUNNING_JOB_RESTART);
                    } else {
                        notice(scriptHistory, Constant.ErrorType.FLINK_STREAM_NO_RUNNING_JOB_RESTART_FAILED);
                    }
                } else {
                    notice(scriptHistory, Constant.ErrorType.FLINK_STREAM_NO_RUNNING_JOB);
                }
            } else {
                // 任务阻塞判断
                if (monitor.getWaitingBatches() == 0) {
                    return;
                }
                // 获取背压数据
                BackpressureInfo backpressure = YarnApiUtils.backpressure(cluster.getYarnUrl(), scriptHistory.getJobId());
                if (backpressure == null) {
                    return;
                }
                // 阈值
                int maxBatches = monitor.getWaitingBatches();
                boolean isOutBatches = backpressure.ratio >= maxBatches;
                // 任务阻塞超阈值-告警
                if (isOutBatches) {
                    // 阻塞重启
                    if (monitor.getBlockingRestart()) {
                        YarnApiUtils.killApp(cluster.getYarnUrl(), scriptHistory.getJobId());
                        scriptHistory.updateState(Constant.JobState.KILLED);
                        scriptHistoryService.save(scriptHistory);
                        boolean restart = restart();
                        if (restart) {
                            notice(scriptHistory, Constant.ErrorType.FLINK_STREAM_BACKPRESSURE_RESTART + "(trouble vertex: " + backpressure.nextVertex + ")");
                        } else {
                            notice(scriptHistory, Constant.ErrorType.FLINK_STREAM_BACKPRESSURE_RESTART_FAILED + "(trouble vertex: " + backpressure.nextVertex + ")");
                        }
                    } else {
                        notice(scriptHistory, Constant.ErrorType.FLINK_STREAM_BACKPRESSURE + "(trouble vertex: " + backpressure.nextVertex + ")");
                    }
                }
            }
        } else {
            // 异常重启
            if (monitor.getExRestart()) {
                boolean restart = restart();
                if (restart) {
                    notice(scriptHistory, String.format(Constant.ErrorType.FLINK_STREAM_UNUSUAL_RESTART, scriptHistory.getJobFinalStatus()));
                } else {
                    notice(scriptHistory, String.format(Constant.ErrorType.FLINK_STREAM_UNUSUAL_RESTART_FAILED, scriptHistory.getJobFinalStatus()));
                }
            } else {
                notice(scriptHistory, String.format(Constant.ErrorType.FLINK_STREAM_UNUSUAL, scriptHistory.getJobFinalStatus()));
            }
        }
    }

    private boolean restart() {
        ScriptHistory scriptHistory = scriptHistoryService.findNoScheduleLatestByScriptId(script.getId());
        if (scriptHistory != null && scriptHistory.isRunning()) {
            return true;
        }
        return scriptService.execute(script, monitor);
    }

    public static void build(Monitor monitor) {
        SchedulerUtils.scheduleCronJob(MonitorJob.class,
                monitor.getId(),
                Constant.JobGroup.MONITOR,
                monitor.generateCron());
    }

}
