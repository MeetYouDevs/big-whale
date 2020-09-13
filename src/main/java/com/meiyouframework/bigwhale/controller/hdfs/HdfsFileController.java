package com.meiyouframework.bigwhale.controller.hdfs;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.meiyouframework.bigwhale.common.pojo.Msg;
import com.meiyouframework.bigwhale.common.pojo.FileStatus;
import com.meiyouframework.bigwhale.entity.Cluster;
import com.meiyouframework.bigwhale.service.ClusterService;
import com.meiyouframework.bigwhale.controller.BaseController;
import com.meiyouframework.bigwhale.security.LoginUser;
import com.meiyouframework.bigwhale.util.WebHdfsUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/hdfs")
public class HdfsFileController extends BaseController {

    @Autowired
    private ClusterService clusterService;

    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public Msg uploadFile(
            MultipartFile file,
            @RequestParam(required = false) String clusterId,
            @RequestParam(required = false) String path) {
        Cluster cluster;
        LoginUser loginUser = getCurrentUser();
        if (StringUtils.isBlank(path)) {
            cluster = clusterService.findOneByQuery("defaultFileCluster=true");
            if (cluster == null) {
                if (StringUtils.isBlank(clusterId)) {
                    return failed("未设置默认储存集群，请先选择运行集群");
                } else {
                    cluster = clusterService.findById(clusterId);
                }
            }
            path = cluster.getFsDir() + "/" + loginUser.getUsername();
        } else {
            path = regularPath(path);
            cluster = clusterService.findById(clusterId);
            if (loginUser.isRoot()) {
                path = cluster.getFsDir() + path;
            } else {
                path = cluster.getFsDir() + "/" + loginUser.getUsername() + path;
            }
        }
        boolean success = WebHdfsUtils.upload(file, cluster.getFsWebhdfs(), path, cluster.getFsUser(), loginUser.getUsername());
        if (success) {
            String fsDefaultFs = cluster.getFsDefaultFs();
            if (fsDefaultFs.endsWith("/")) {
                fsDefaultFs = fsDefaultFs.substring(0, fsDefaultFs.length() - 1);
            }
            String hdfsPath = fsDefaultFs + path + "/" + file.getOriginalFilename();
            return success(hdfsPath);
        } else {
            return failed();
        }
    }

    @RequestMapping(value = "/download", method = RequestMethod.GET)
    public void downFile(
            HttpServletResponse response,
            @RequestParam String clusterId,
            @RequestParam String path) throws UnsupportedEncodingException {
        path = regularPath(path);
        String name = path.substring(path.lastIndexOf('/') + 1);
        response.setContentType("application/force-download;charset=UTF-8");
        response.addHeader("Content-Disposition", "attachment;fileName=" + new String(name.getBytes(), "ISO8859-1"));
        Cluster cluster = clusterService.findById(clusterId);
        LoginUser loginUser = getCurrentUser();
        if (loginUser.isRoot()) {
            path = cluster.getFsDir() + path;
        } else {
            path = cluster.getFsDir() + "/" + loginUser.getUsername() + path;
        }
        WebHdfsUtils.download(response, cluster.getFsWebhdfs(), path, loginUser.getUsername());
    }

    @RequestMapping(value = "/delete.api", method = RequestMethod.POST)
    public Msg delete(
            @RequestParam String clusterId,
            @RequestParam String path) {
        path = regularPath(path);
        Cluster cluster = clusterService.findById(clusterId);
        LoginUser loginUser = getCurrentUser();
        int result;
        if (loginUser.isRoot()) {
            path = cluster.getFsDir() + path;
            result = WebHdfsUtils.delete(cluster.getFsWebhdfs(), path, cluster.getFsUser());
        } else {
            path = cluster.getFsDir() + '/' + loginUser.getUsername() + path;
            result = WebHdfsUtils.delete(cluster.getFsWebhdfs(), path, loginUser.getUsername());
        }
        if (result != 200) {
            if (result == 403) {
                return failed("无操作权限");
            } else {
                return failed();
            }
        }
        return success();
    }

    @RequestMapping(value = "/list.api", method = RequestMethod.GET)
    public Msg list(
            @RequestParam String clusterId,
            @RequestParam String path) {
        path = regularPath(path);
        Cluster cluster = clusterService.findById(clusterId);
        LoginUser loginUser = getCurrentUser();
        if (loginUser.isRoot()) {
            path = cluster.getFsDir() + path;
        } else {
            path = cluster.getFsDir() + "/" + loginUser.getUsername() + path;
        }
        JSONArray fileStatuses = WebHdfsUtils.list(cluster.getFsWebhdfs(), path, loginUser.isRoot() ? cluster.getFsUser() : loginUser.getUsername());
        List<FileStatus> dtos = new ArrayList<>();
        if (fileStatuses != null) {
            String fsDefaultFs = cluster.getFsDefaultFs();
            if (fsDefaultFs.endsWith("/")) {
                fsDefaultFs = fsDefaultFs.substring(0, fsDefaultFs.length() - 1);
            }
            String hdfsPathPrefix = fsDefaultFs + path + "/";
            for (Object fileStatus : fileStatuses) {
                FileStatus fileStatusDto = FileStatus.builder()
                        .id(UUID.randomUUID().toString())
                        .name(((JSONObject) fileStatus).getString("pathSuffix"))
                        .path(hdfsPathPrefix + ((JSONObject) fileStatus).getString("pathSuffix"))
                        .length(((JSONObject) fileStatus).getLongValue("length"))
                        .isdir("DIRECTORY".equals(((JSONObject) fileStatus).getString("type")))
                        .modification_time(new Date(((JSONObject) fileStatus).getLongValue("modificationTime")))
                        .permission(((JSONObject) fileStatus).getString("permission"))
                        .owner(((JSONObject) fileStatus).getString("owner"))
                        .group(((JSONObject) fileStatus).getString("group"))
                        .build();
                dtos.add(fileStatusDto);
            }
        }
        return success(dtos);
    }

    private String regularPath(String path) {
        if ("/".equals(path)) {
            return "";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

}