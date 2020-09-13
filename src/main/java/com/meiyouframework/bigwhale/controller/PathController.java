package com.meiyouframework.bigwhale.controller;

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

    @GetMapping("/script/list.html")
    public String scriptPage() {
        return "script/list";
    }

    @GetMapping("/script/edit.html")
    public String scriptAdd(
            @RequestParam(name = "id", required = false) String id,
            Model viewObjects) {
        if (StringUtils.isNotBlank(id)) {
            viewObjects.addAttribute("scriptId", id);
        }
        return "script/edit";
    }

    @GetMapping("/scheduling/list.html")
    public String taskPage() {
        return "scheduling/list";
    }

    @GetMapping("/scheduling/edit.html")
    public String taskAdd(@RequestParam(name = "id", required = false) String id,
                          Model viewObjects) {
        if (StringUtils.isNotBlank(id)) {
            viewObjects.addAttribute("schedulingId", id);
        }
        return "scheduling/edit";
    }

    @GetMapping("/yarn_app/list.html")
    public String jobYarnPage(
            @RequestParam(name = "appId", required = false) String appId,
            Model viewObjects) {
        if (StringUtils.isNotBlank(appId)) {
            viewObjects.addAttribute("appId", appId);
        }
        return "yarn_app/list";
    }

    @GetMapping("/script/cmd_record/list.html")
    public String cmdRecordPage(
            @RequestParam(name = "id", required = false) String id,
            @RequestParam(name = "scriptId", required = false) String scriptId,
            @RequestParam(name = "schedulingId", required = false) String schedulingId,
            Model viewObjects) {
        if (StringUtils.isNotBlank(id)) {
            viewObjects.addAttribute("id", id);
        }
        if (StringUtils.isNotBlank(scriptId)) {
            viewObjects.addAttribute("scriptId", scriptId);
        }
        if (StringUtils.isNotBlank(schedulingId)) {
            viewObjects.addAttribute("schedulingId", schedulingId);
        }
        return "script/cmd_record/list";
    }

    @GetMapping("/hdfs/list.html")
    public String hdfsPage() {
        return "hdfs/list";
    }

    @GetMapping("/admin/cluster/list.html")
    public String clusterPage() {
        return "admin/cluster/list";
    }

    @GetMapping("/admin/cluster/cluster_user/list.html")
    public String clusterUserPage(
            @RequestParam(name = "clusterId", required = false) String clusterId,
            Model model) {
        if (StringUtils.isNotBlank(clusterId)) {
            model.addAttribute("clusterId", clusterId);
        }
        return "admin/cluster/cluster_user/list";
    }

    @GetMapping("/admin/cluster/agent/list.html")
    public String agentPage() {
        return "admin/cluster/agent/list";
    }

    @GetMapping("/admin/cluster/compute_framework/list.html")
    public String computeFrameworkPage() {
        return "admin/cluster/compute_framework/list";
    }

    @GetMapping("/auth/resource/list.html")
    public String resourcesPage() {
        return "admin/auth/resource/list";
    }

    @GetMapping("/auth/role/list.html")
    public String rolesPage() {
        return "admin/auth/role/list";
    }

    @GetMapping("/auth/user/list.html")
    public String usersPage() {
        return "admin/auth/user/list";
    }

}
