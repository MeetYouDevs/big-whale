package com.meiyou.bigwhale.controller;

import org.apache.catalina.util.ServerInfo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PathController {

    @Autowired
    private MultipartProperties properties;

    @GetMapping(value = "/login.html")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/index.html")
    public String index1() {
        return "index";
    }

    @GetMapping("/welcome.html")
    public String welcome(Model model) {
        model.addAttribute("osName", System.getProperty("os.name"));
        model.addAttribute("osArch", System.getProperty("os.arch"));
        model.addAttribute("osVersion", System.getProperty("os.version"));
        model.addAttribute("javaVersion", System.getProperty("java.version"));
        model.addAttribute("tomcatVersion", ServerInfo.getServerInfo());
        model.addAttribute("maxFileSize", properties.getMaxFileSize());
        return "welcome";
    }

    @GetMapping("/schedule/list.html")
    public String scheduleList() {
        return "schedule/list";
    }

    @GetMapping("/schedule/edit.html")
    public String scheduleEdit(
            @RequestParam(name = "id", required = false) Integer id,
            Model viewObjects) {
        if (id != null) {
            viewObjects.addAttribute("scheduleId", id);
        }
        return "schedule/edit";
    }

    @GetMapping("/schedule/instance.html")
    public String scheduleInstance(
            @RequestParam(name = "id", required = false) Integer id,
            @RequestParam(name = "instanceId", required = false) String instanceId,
            Model viewObjects) {
        if (id != null) {
            viewObjects.addAttribute("scheduleId", id);
        }
        if (StringUtils.isNotBlank(instanceId)) {
            viewObjects.addAttribute("instanceId", instanceId);
        }
        return "schedule/instance";
    }

    @GetMapping("/stream/list.html")
    public String streamList() {
        return "stream/list";
    }

    @GetMapping("/stream/edit.html")
    public String streamEdit(
            @RequestParam(name = "id", required = false) Integer id,
            Model viewObjects) {
        if (id != null) {
            viewObjects.addAttribute("scriptId", id);
        }
        return "stream/edit";
    }

    @GetMapping("/script_history/list.html")
    public String scriptHistoryList(
            @RequestParam(name = "id", required = false) Integer id,
            @RequestParam(name = "scheduleId", required = false) Integer scheduleId,
            @RequestParam(name = "scheduleTopNodeId", required = false) String scheduleTopNodeId,
            @RequestParam(name = "scheduleInstanceId", required = false) String scheduleInstanceId,
            @RequestParam(name = "scriptId", required = false) Integer scriptId,
            Model viewObjects) {
        if (id != null) {
            viewObjects.addAttribute("id", id);
        }
        if (scheduleId != null) {
            viewObjects.addAttribute("scheduleId", scheduleId);
            viewObjects.addAttribute("scheduleTopNodeId", scheduleTopNodeId);
            viewObjects.addAttribute("scheduleInstanceId", scheduleInstanceId);
        }
        if (scriptId != null) {
            viewObjects.addAttribute("scriptId", scriptId);
            viewObjects.addAttribute("scriptId_", "-");
        }
        return "script_history/list";
    }

    @GetMapping("/hdfs/list.html")
    public String hdfsList() {
        return "hdfs/list";
    }

    @GetMapping("/yarn_app/list.html")
    public String yarnList() {
        return "yarn_app/list";
    }

    @GetMapping("/admin/cluster/list.html")
    public String clusterList() {
        return "admin/cluster/list";
    }

    @GetMapping("/admin/cluster/cluster_user/list.html")
    public String clusterUserList() {
        return "admin/cluster/cluster_user/list";
    }

    @GetMapping("/admin/cluster/agent/list.html")
    public String agentList() {
        return "admin/cluster/agent/list";
    }

    @GetMapping("/admin/cluster/compute_framework/list.html")
    public String computeFrameworkList() {
        return "admin/cluster/compute_framework/list";
    }

    @GetMapping("/auth/resource/list.html")
    public String resourceList() {
        return "admin/auth/resource/list";
    }

    @GetMapping("/auth/role/list.html")
    public String roleList() {
        return "admin/auth/role/list";
    }

    @GetMapping("/auth/user/list.html")
    public String userList() {
        return "admin/auth/user/list";
    }

}
