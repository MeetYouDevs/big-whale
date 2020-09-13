package com.meiyouframework.bigwhale.task;

import com.meiyouframework.bigwhale.common.Constant;
import com.meiyouframework.bigwhale.common.pojo.HttpYarnApp;
import com.meiyouframework.bigwhale.entity.*;
import com.meiyouframework.bigwhale.entity.auth.User;
import com.meiyouframework.bigwhale.service.*;
import com.meiyouframework.bigwhale.service.auth.UserService;
import com.meiyouframework.bigwhale.util.MsgTools;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @author Suxy
 * @date 2020/4/24
 * @description file description
 */
public abstract class AbstractNoticeableTask {

    @Autowired
    private UserService userService;
    @Autowired
    private ScriptService scriptService;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private AgentService agentService;
    @Autowired
    private NoticeService noticeService;
    @Autowired
    private ClusterUserService clusterUserService;

    protected void notice(CmdRecord cmdRecord, Scheduling scheduling, String appId, String errorType) {
        Script script = null;
        User user = null;
        String email = null;
        String dingDingHooks = null;
        if (scheduling != null) {
            if (scheduling.getType() == Constant.SCHEDULING_TYPE_STREAMING) {
                script = scriptService.findById(scheduling.getScriptIds());
            } else {
                script = scriptService.findById(cmdRecord.getScriptId());
            }
            user = userService.findById(scheduling.getUid());
            email = scheduling.getSendEmail() ? user.getEmail() : null;
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
        if (script.getType() != Constant.SCRIPT_TYPE_SHELL_BATCH) {
            Cluster cluster = clusterService.findById(script.getClusterId());
            String trackingUrl = cluster.getYarnUrl();
            if (StringUtils.isNotBlank(appId)) {
                trackingUrl = trackingUrl + "/cluster/app/" + appId;
            }
            content = MsgTools.getPlainErrorMsg(cluster.getName(), trackingUrl, userName, script.getName(), errorType);
        } else {
            Agent agent = agentService.findById(script.getAgentId());
            content = MsgTools.getPlainErrorMsg(agent.getName(), userName, script.getName(), errorType);
        }
        //发送邮件
        if (StringUtils.isNotBlank(email)) {
            noticeService.sendEmail(email, content);
        }
        //发送钉钉
        boolean publicWatchFlag = scheduling != null;
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
            user = userService.findById(script.getUid());
            String userName = user.getNickname() != null ? user.getNickname() : user.getUsername();
            msg = MsgTools.getPlainErrorMsg(cluster.getName(), trackingUrls, userName, script.getName(), errorType);
        } else {
            String queue = httpYarnApp.getQueue();
            List<ClusterUser> clusterUsers = clusterUserService.findByQuery("clusterId=" + cluster.getId() + ";queue=" + queue);
            if (clusterUsers.isEmpty()) {
                if (queue.startsWith("root.")) {
                    queue = queue.substring(5);
                    clusterUsers = clusterUserService.findByQuery("clusterId=" + cluster.getId() + ";queue=" + queue);
                }
            }
            if (!clusterUsers.isEmpty()) {
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
        if (user != null && StringUtils.isNotBlank(user.getEmail())) {
            noticeService.sendEmail(user.getEmail(), msg);
        }
        noticeService.sendDingding(new String[0], msg);
    }
}
