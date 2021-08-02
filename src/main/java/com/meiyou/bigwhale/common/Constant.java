package com.meiyou.bigwhale.common;

/**
 * @author progr1mmer
 */
public interface Constant {

    /**
     * 脚本类型
     */
    interface ScriptType {
        String SHELL = "shell";
        String SPARK_BATCH = "sparkbatch";
        String FLINK_BATCH = "flinkbatch";
        String SPARK_STREAM = "sparkstream";
        String FLINK_STREAM = "flinkstream";
    }

    /**
     * Job分组
     */
    interface JobGroup {
        String COMMON = "common";
        String MONITOR = "monitor";
        String SCHEDULE = "schedule";
        String SCRIPT_JOB = "scriptJob";
    }

    /**
     * 执行状态
     */
    interface JobState {

        /**
         * 调度扩展执行状态
         */
        String UN_CONFIRMED_ = "UN_CONFIRMED";
        String TIME_WAIT_ = "TIME_WAIT";

        String SUBMIT_WAIT = "SUBMIT_WAIT";
        String SUBMITTING = "SUBMITTING";
        String SUBMITTED = "SUBMITTED";
        String ACCEPTED = "ACCEPTED";
        String RUNNING = "RUNNING";
        String SUCCEEDED = "SUCCEEDED";
        String KILLED = "KILLED";
        String FAILED = "FAILED";
        String TIMEOUT = "TIMEOUT";

    }

    /**
     * 任务告警
     */
    interface ErrorType {
        /**
         * 告警信息
         */
        String FAILED = "脚本执行失败";
        String TIMEOUT = "脚本执行超时";

        String SPARK_BATCH_UNUSUAL = "spark离线任务异常(%s)";
        String SPARK_STREAM_WAITING_BATCH = "spark实时任务批次积压";
        String SPARK_STREAM_WAITING_BATCH_RESTART = "spark实时任务批次积压，已重启";
        String SPARK_STREAM_WAITING_BATCH_RESTART_FAILED = "spark实时任务批次积压，重启失败";
        String SPARK_STREAM_UNUSUAL = "spark实时任务异常(%s)";
        String SPARK_STREAM_UNUSUAL_RESTART = "spark实时任务异常(%s)，已重启";
        String SPARK_STREAM_UNUSUAL_RESTART_FAILED = "spark实时任务异常(%s)，重启失败";

        String FLINK_BATCH_UNUSUAL = "flink离线任务异常(%s)";
        String FLINK_STREAM_NO_RUNNING_JOB = "flink实时任务无运行中的job";
        String FLINK_STREAM_NO_RUNNING_JOB_RESTART = "flink实时任务无运行中的job，已重启";
        String FLINK_STREAM_NO_RUNNING_JOB_RESTART_FAILED = "flink实时任务无运行中的job，重启失败";
        String FLINK_STREAM_BACKPRESSURE = "flink实时任务阻塞";
        String FLINK_STREAM_BACKPRESSURE_RESTART = "flink实时任务阻塞，已重启";
        String FLINK_STREAM_BACKPRESSURE_RESTART_FAILED = "flink实时任务阻塞，重启失败";
        String FLINK_STREAM_UNUSUAL = "flink实时任务异常(%s)";
        String FLINK_STREAM_UNUSUAL_RESTART = "flink实时任务异常(%s)，已重启";
        String FLINK_STREAM_UNUSUAL_RESTART_FAILED = "flink实时任务异常(%s)，重启失败";

        String SERVER_UNEXPECTED_EXIT = "服务异常退出";
        String APP_DUPLICATE = "应用重复";
        String APP_NO_RUNNING = "应用未运行";
        String APP_MEMORY_OVERLIMIT = "内存超限";
    }

    /**
     * 调度任务可视化时间维度
     */
    int TIMER_CYCLE_MINUTE = 1;
    int TIMER_CYCLE_HOUR = 2;
    int TIMER_CYCLE_DAY = 3;
    int TIMER_CYCLE_WEEK = 4;

    /**
     * 钉钉机器人消息API
     */
    String DINGDING_ROBOT_URL = "https://oapi.dingtalk.com/robot/send?access_token=";

}
