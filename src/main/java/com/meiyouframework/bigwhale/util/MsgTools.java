package com.meiyouframework.bigwhale.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MsgTools {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private MsgTools() {

    }

    public static String getPlainErrorMsg(String clusterName, String trackingUrl, String userName, String taskName, String errorType) {
        StringBuilder msg = new StringBuilder("<告警信息>\n");
        if (clusterName != null) {
            msg.append("集群: ").append(clusterName).append("\n");
        }
        if (errorType != null) {
            msg.append("类型: ").append(errorType).append("\n");
        }
        if (userName != null) {
            msg.append("用户: ").append(userName).append("\n");
        }
        if (taskName != null) {
            msg.append("任务: ").append(taskName).append("\n");
        }
        if (trackingUrl != null) {
            msg.append("url: ").append(trackingUrl).append("\n");
        }
        msg.append("时间: ").append(DATE_FORMAT.format(new Date())).append("\n");
        return msg.toString();
    }

    public static String getPlainErrorMsg(String agentName, String userName, String taskName, String errorType) {
        StringBuilder msg = new StringBuilder("<告警信息>\n");
        if (agentName != null) {
            msg.append("代理: ").append(agentName).append("\n");
        }
        if (errorType != null) {
            msg.append("类型: ").append(errorType).append("\n");
        }
        if (userName != null) {
            msg.append("用户: ").append(userName).append("\n");
        }
        if (taskName != null) {
            msg.append("任务: ").append(taskName).append("\n");
        }
        msg.append("时间: ").append(DATE_FORMAT.format(new Date())).append("\n");
        return msg.toString();
    }

}
