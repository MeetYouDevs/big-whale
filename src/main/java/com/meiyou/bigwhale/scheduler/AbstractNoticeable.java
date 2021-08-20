package com.meiyou.bigwhale.scheduler;

import com.meiyou.bigwhale.entity.*;
import com.meiyou.bigwhale.service.*;
import com.meiyou.bigwhale.entity.auth.User;
import com.meiyou.bigwhale.service.auth.UserService;
import com.meiyou.bigwhale.util.MsgTools;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Suxy
 * @date 2020/4/24
 * @description file description
 */
public abstract class AbstractNoticeable {

    @Autowired
    private UserService userService;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private AgentService agentService;
    @Autowired
    private NoticeService noticeService;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private MonitorService monitorService;

    protected void notice(String taskName, String errorType) {
        String msg = MsgTools.getPlainErrorMsg(null, null, null, taskName, errorType);
        noticeService.sendDingding(new String[0], msg);
    }

    /**
     * 统一按照最后的配置进行报警处理
     * @param scriptHistory
     * @param errorType
     */
    protected void notice(ScriptHistory scriptHistory, String errorType) {
        User user = userService.findById(scriptHistory.getCreateBy());
        String userName = user.getNickname() == null ? user.getUsername() : user.getNickname();
        String taskName = null;
        String email = null;
        String dingDingHooks = null;
        if (scriptHistory.getScheduleId() != null) {
            Schedule schedule = scheduleService.findById(scriptHistory.getScheduleId());
            if (schedule == null) {
                return;
            }
            taskName = schedule.getName() + " - " + scriptHistory.getScriptName();
            email = schedule.getSendEmail() ? user.getEmail() : null;
            dingDingHooks = schedule.getDingdingHooks();
        } else if (scriptHistory.getMonitorId() != null) {
            Monitor monitor = monitorService.findById(scriptHistory.getMonitorId());
            if (monitor == null) {
                return;
            }
            taskName = scriptHistory.getScriptName();
            email = monitor.getSendEmail() ? user.getEmail() : null;
            dingDingHooks = monitor.getDingdingHooks();
        }
        if (StringUtils.isBlank(email) && StringUtils.isBlank(dingDingHooks)) {
            return;
        }
        String content;
        if (scriptHistory.getClusterId() != null) {
            Cluster cluster = clusterService.findById(scriptHistory.getClusterId());
            content = MsgTools.getPlainErrorMsg(cluster.getName(), scriptHistory.getJobUrl(), userName, taskName, errorType);
        } else {
            Agent agent = agentService.findById(scriptHistory.getAgentId());
            content = MsgTools.getPlainErrorMsg(agent.getName(), userName, taskName, errorType);
        }
        //发送邮件
        if (StringUtils.isNotBlank(email)) {
            noticeService.sendEmail(email, content);
        }
        //发送钉钉
        boolean publicWatchFlag = scriptHistory.getScheduleId() != null || scriptHistory.getMonitorId() != null;
        if (StringUtils.isNotBlank(dingDingHooks)) {
            String[] ats = null;
            for (String token : dingDingHooks.split(",")) {
                // 已添加告警到公共群的不重复发告警
                if (noticeService.isWatcherToken(token)) {
                    publicWatchFlag = false;
                }
                if (token.contains("&")) {
                    String [] tokenArr = token.split("&");
                    if (tokenArr.length >= 2 && StringUtils.isNotBlank(tokenArr[1])) {
                        ats = tokenArr[1].substring(1).split("@");
                    }
                } else {
                    if (StringUtils.isNotBlank(user.getPhone())) {
                        ats = new String[]{user.getPhone()};
                    }
                }
                noticeService.sendDingding(token, ats, content);
            }
        }
        if (publicWatchFlag) {
            //发送告警到公共群
            noticeService.sendDingding(new String[0], content);
        }
    }

    protected void notice(Cluster cluster, YarnApp yarnApp, String trackingUrls, String errorType) {
        User user = null;
        String msg;
        if (yarnApp.getUserId() != null) {
            user = userService.findById(yarnApp.getUserId());
            String userName = user.getNickname() != null ? user.getNickname() : user.getUsername();
            msg = MsgTools.getPlainErrorMsg(cluster.getName(), trackingUrls, userName, yarnApp.getName(), errorType);
        } else {
            msg = MsgTools.getPlainErrorMsg(cluster.getName(), trackingUrls, yarnApp.getUser(), yarnApp.getName(), errorType);
        }
        if (user != null && StringUtils.isNotBlank(user.getEmail())) {
            noticeService.sendEmail(user.getEmail(), msg);
        }
        noticeService.sendDingding(new String[0], msg);
    }

}
