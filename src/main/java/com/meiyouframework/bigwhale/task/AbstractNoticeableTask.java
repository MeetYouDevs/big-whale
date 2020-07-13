package com.meiyouframework.bigwhale.task;

import com.meiyouframework.bigwhale.common.Constant;
import com.meiyouframework.bigwhale.common.pojo.HttpYarnApp;
import com.meiyouframework.bigwhale.entity.*;
import com.meiyouframework.bigwhale.entity.auth.User;
import com.meiyouframework.bigwhale.service.*;
import com.meiyouframework.bigwhale.service.auth.UserService;
import com.meiyouframework.bigwhale.util.MsgTools;
import com.meiyouframework.bigwhale.util.SpringContextUtils;
import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * @author Suxy
 * @date 2020/4/24
 * @description file description
 */
public abstract class AbstractNoticeableTask {

    protected void notice(CmdRecord cmdRecord, Monitor monitor, Scheduling scheduling, String appId, String errorType) {
        UserService userService = SpringContextUtils.getBean(UserService.class);
        ScriptService scriptService = SpringContextUtils.getBean(ScriptService.class);
        Script script = null;
        User user = null;
        String email = null;
        String dingDingHooks = null;
        if (monitor != null) {
            script = scriptService.findById(monitor.getScriptId());
            user = userService.findById(monitor.getUid());
            email = monitor.getSendMail() ? user.getEmail() : null;
            dingDingHooks = monitor.getDingdingHooks();
        } else if (scheduling != null) {
            script = scriptService.findById(cmdRecord.getScriptId());
            user = userService.findById(scheduling.getUid());
            email = scheduling.getSendMail() ? user.getEmail() : null;
            dingDingHooks = scheduling.getDingdingHooks();
        } else if (cmdRecord != null) {
            //手动执行
            script = scriptService.findById(cmdRecord.getScriptId());
            user = userService.findById(cmdRecord.getUid());
            email = user.getEmail();
        }
        if (StringUtils.isBlank(email) && StringUtils.isBlank(dingDingHooks)) {
            return;
        }
        String userName = user.getNickname() == null ? user.getUsername() : user.getNickname();
        String content;
        if (script.getType() != Constant.SCRIPT_TYPE_SHELL) {
            ClusterService clusterService = SpringContextUtils.getBean(ClusterService.class);
            Cluster cluster = clusterService.findById(script.getClusterId());
            String trackingUrl = cluster.getYarnUrl();
            if (StringUtils.isNotBlank(appId)) {
                trackingUrl = trackingUrl + "/cluster/app/" + appId;
            }
            content = MsgTools.getPlainErrorMsg(cluster.getName(), trackingUrl, userName, script.getName(), errorType);
        } else {
            AgentService agentService = SpringContextUtils.getBean(AgentService.class);
            Agent agent = agentService.findById(script.getAgentId());
            content = MsgTools.getPlainErrorMsg(agent.getIp(), userName, script.getName(), errorType);
        }
        NoticeService noticeService = SpringContextUtils.getBean(NoticeService.class);
        //发送邮件
        if (StringUtils.isNotBlank(email)) {
            noticeService.sendMail(email, content);
        }
        //发送钉钉
        boolean publicWatchFlag = monitor != null || scheduling != null;
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

    protected void notice(Cluster cluster, Script script, HttpYarnApp httpYarnApp, String trackingUrls, String errorType) {
        User user = null;
        String msg;
        if (script != null) {
            UserService userService = SpringContextUtils.getBean(UserService.class);
            user = userService.findById(script.getUid());
            String userName = user.getNickname() != null ? user.getNickname() : user.getUsername();
            msg = MsgTools.getPlainErrorMsg(cluster.getName(), trackingUrls, userName, script.getName(), errorType);
        } else {
            ClusterUserService clusterUserService = SpringContextUtils.getBean(ClusterUserService.class);
            String queue = httpYarnApp.getQueue();
            List<ClusterUser> clusterUsers = clusterUserService.findByQuery("clusterId=" + cluster.getId() + ";queue=" + queue);
            if (clusterUsers.isEmpty()) {
                if (queue.startsWith("root.")) {
                    queue = queue.substring(5);
                    clusterUsers = clusterUserService.findByQuery("clusterId=" + cluster.getId() + ";queue=" + queue);
                }
            }
            if (!clusterUsers.isEmpty()) {
                UserService userService = SpringContextUtils.getBean(UserService.class);
                boolean match = false;
                for (ClusterUser clusterUser : clusterUsers) {
                    if (httpYarnApp.getUser().equals(clusterUser.getUser())) {
                        user = userService.findById(clusterUser.getUid());
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    user = userService.findById(clusterUsers.get(0).getUid());
                }
                String userName = user.getNickname() != null ? user.getNickname() : user.getUsername();
                msg = MsgTools.getPlainErrorMsg(cluster.getName(), trackingUrls, userName, httpYarnApp.getName(), errorType);
            } else {
                msg = MsgTools.getPlainErrorMsg(cluster.getName(), trackingUrls, httpYarnApp.getUser(), httpYarnApp.getName(), errorType);
            }
        }
        NoticeService noticeService = SpringContextUtils.getBean(NoticeService.class);
        if (user != null && StringUtils.isNotBlank(user.getEmail())) {
            noticeService.sendMail(user.getEmail(), msg);
        }
        noticeService.sendDingding(new String[0], msg);
    }
}
