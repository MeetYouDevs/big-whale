package com.meiyouframework.bigwhale.common;

/**
 * @author progr1mmer
 */
public interface Constant {

    /**
     * 脚本类型
     */
    int SCRIPT_TYPE_SHELL_BATCH = 0;
    int SCRIPT_TYPE_SPARK_STREAMING = 1;
    int SCRIPT_TYPE_SPARK_BATCH = 2;
    int SCRIPT_TYPE_FLINK_STREAMING = 3;
    int SCRIPT_TYPE_FLINK_BATCH = 4;

    /**
     * 执行状态
     */
    int EXEC_STATUS_UNSTART = 1;
    int EXEC_STATUS_DOING = 2;
    int EXEC_STATUS_FINISH = 3;
    int EXEC_STATUS_TIMEOUT = 4;
    int EXEC_STATUS_FAIL = 5;

    /**
     * 任务调度类型
     */
    int SCHEDULING_TYPE_BATCH = 0;
    int SCHEDULING_TYPE_STREAMING = 1;
    /**
     * 调度任务可视化时间维度
     */
    int TIMER_CYCLE_MINUTE = 1;
    int TIMER_CYCLE_HOUR = 2;
    int TIMER_CYCLE_DAY = 3;
    int TIMER_CYCLE_WEEK = 4;

    /**
     * 告警信息
     */
    String ERROR_TYPE_FAILED = "脚本执行失败";
    String ERROR_TYPE_TIMEOUT = "脚本执行超时";

    String ERROR_TYPE_SPARK_STREAMING_WAITING_BATCH = "spark实时任务批次积压";
    String ERROR_TYPE_SPARK_STREAMING_WAITING_BATCH_RESTART = "spark实时任务批次积压，已重启";
    String ERROR_TYPE_SPARK_STREAMING_WAITING_BATCH_RESTART_FAILED = "spark实时任务批次积压，重启失败";
    String ERROR_TYPE_SPARK_STREAMING_UNUSUAL = "spark实时任务异常(%s)";
    String ERROR_TYPE_SPARK_STREAMING_UNUSUAL_RESTART = "spark实时任务异常(%s)，已重启";
    String ERROR_TYPE_SPARK_STREAMING_UNUSUAL_RESTART_FAILED = "spark实时任务异常(%s)，重启失败";
    String ERROR_TYPE_SPARK_BATCH_UNUSUAL = "spark离线任务异常(%s)";

    String ERROR_TYPE_FLINK_STREAMING_NO_RUNNING_JOB = "flink实时任务无运行中的job";
    String ERROR_TYPE_FLINK_STREAMING_NO_RUNNING_JOB_RESTART = "flink实时任务无运行中的job，已重启";
    String ERROR_TYPE_FLINK_STREAMING_NO_RUNNING_JOB_RESTART_FAILED = "flink实时任务无运行中的job，重启失败";
    String ERROR_TYPE_FLINK_STREAMING_BACKPRESSURE = "flink实时任务阻塞";
    String ERROR_TYPE_FLINK_STREAMING_BACKPRESSURE_RESTART = "flink实时任务阻塞，已重启";
    String ERROR_TYPE_FLINK_STREAMING_BACKPRESSURE_RESTART_FAILED = "flink实时任务阻塞，重启失败";
    String ERROR_TYPE_FLINK_STREAMING_UNUSUAL = "flink实时任务异常(%s)";
    String ERROR_TYPE_FLINK_STREAMING_UNUSUAL_RESTART = "flink实时任务异常(%s)，已重启";
    String ERROR_TYPE_FLINK_STREAMING_UNUSUAL_RESTART_FAILED = "flink实时任务异常(%s)，重启失败";
    String ERROR_TYPE_FLINK_BATCH_UNUSUAL = "flink离线任务异常(%s)";

    String ERROR_TYPE_APP_DUPLICATE = "应用重复";
    String ERROR_TYPE_APP_MEMORY_OVERLIMIT = "内存超限";

    /**
     * 钉钉机器人消息API
     */
    String DINGDING_ROBOT_URL = "https://oapi.dingtalk.com/robot/send?access_token=";

    /**
     * Job分组
     */
    interface JobGroup {
        String COMMON = "common";
        String MONITOR = "monitor";
        String TIMED = "timed";
        String TIMED_FOR_API = "timedForApi";
        String CMD = "cmd";
    }

    String APP_APPEND_SYMBOL = "$";

}
