package com.meiyouframework.bigwhale.controller.script;

import com.meiyouframework.bigwhale.common.Constant;
import com.meiyouframework.bigwhale.common.pojo.Msg;
import com.meiyouframework.bigwhale.config.SshConfig;
import com.meiyouframework.bigwhale.entity.*;
import com.meiyouframework.bigwhale.util.YarnApiUtils;
import com.meiyouframework.bigwhale.config.YarnConfig;
import com.meiyouframework.bigwhale.dto.DtoScript;
import com.meiyouframework.bigwhale.service.*;
import com.meiyouframework.bigwhale.controller.BaseController;
import com.meiyouframework.bigwhale.security.LoginUser;
import com.meiyouframework.bigwhale.task.cmd.CmdRecordRunner;
import org.apache.commons.lang.StringUtils;
import org.quartz.SchedulerException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

import static com.meiyouframework.bigwhale.common.Constant.APP_APPEND_SYMBOL;

@RestController
@RequestMapping("/script")
public class ScriptController extends BaseController {

    @Autowired
    private ScriptService scriptService;
    @Autowired
    private SchedulingService schedulingService;
    @Autowired
    private YarnConfig yarnConfig;
    @Autowired
    private ClusterUserService clusterUserService;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private SshConfig sshConfig;
    @Autowired
    private AgentService agentService;
    @Autowired
    private CmdRecordService cmdRecordService;

    @RequestMapping(value = "/getpage.api", method = RequestMethod.POST)
    public Msg getPage(@RequestBody DtoScript req) {
        LoginUser user = getCurrentUser();
        if (!user.isRoot()) {
            req.setUid(user.getId());
        }
        Page<DtoScript> dtoScriptPage = scriptService.fuzzyPage(req).map((item) -> {
            DtoScript dtoScript = new DtoScript();
            BeanUtils.copyProperties(item, dtoScript);
            if (item.getType() != Constant.SCRIPT_TYPE_SHELL_BATCH) {
                Map<String, Integer> result = calResource(item.getType(), item.getScript());
                dtoScript.setTotalMemory(result.get("totalMemory"));
                dtoScript.setTotalCores(result.get("totalCores"));
            } else {
                dtoScript.setTotalMemory(-1);
                dtoScript.setTotalCores(-1);
            }
            return dtoScript;
        });
        return success(dtoScriptPage);
    }

    @RequestMapping(value = "/getall.api", method = RequestMethod.GET)
    public Msg getAll() {
        LoginUser user = getCurrentUser();
        List<Script> scripts;
        if (!user.isRoot()) {
            scripts = scriptService.findByQuery("uid=" + user.getId());
        } else {
            scripts = scriptService.findByQuery(null);
        }
        List<DtoScript> dtoScripts = scripts.stream().map((item) -> {
            DtoScript dtoScript = new DtoScript();
            BeanUtils.copyProperties(item, dtoScript);
            return dtoScript;
        }).collect(Collectors.toList());
        return success(dtoScripts);
    }

    @RequestMapping(value = "/save.api", method = RequestMethod.POST)
    public Msg saveOrUpdate(@RequestBody DtoScript req) {
        String msg = req.validate();
        if (msg != null) {
            return failed(msg);
        }
        LoginUser user = getCurrentUser();
        if (!user.isRoot()) {
            req.setUid(user.getId());
        }
        //检查脚本是否合法
        msg = checkLegal(req);
        if (msg != null) {
            return failed(msg);
        }
        //应用内存资源参数检查和补充必要参数
        int type = req.getType();
        if (type != Constant.SCRIPT_TYPE_SHELL_BATCH) {
            if (yarnConfig.getAppMemoryThreshold() > 0 && !yarnConfig.getAppWhiteList().contains(req.getApp())) {
                try {
                    int totalMemory = calResource(req.getType(), req.getScript()).get("totalMemory");
                    if (totalMemory > yarnConfig.getAppMemoryThreshold()) {
                        //超过阀值
                        return failed("内存参数配置达到大内存应用标准【" + yarnConfig.getAppMemoryThreshold() + "MB】 ！请调小内存参数或联系管理员升级为大内存应用。");
                    }
                } catch (NumberFormatException e) {
                    return failed("内存参数请向上取整");
                }
            }
            appendNecessaryArgs(req);
        }
        Date now = new Date();
        if (req.getId() == null) {
            req.setCreateTime(now);
        } else {
            Script dbScript = scriptService.findById(req.getId());
            if (dbScript == null) {
                return failed();
            }
            if (req.isBatch() != dbScript.isBatch()) {
                Scheduling scheduling = schedulingService.findOneByQuery("scriptIds?" + req.getId());
                if (scheduling != null) {
                    return failed("变更处理类型前请先移除相关任务调度");
                }
            }
            if (dbScript.getType() != Constant.SCRIPT_TYPE_SHELL_BATCH) {
                //更换脚本类型、集群或队列时检查应用是否正在运行
                if (checkYarnAppAliveIfChangeClusterOrQueue(dbScript, req)) {
                    return failed( "更换脚本类型、集群或队列前请先关闭正在运行的应用");
                }
                //检查程序包是否变更
                if (req.getType() != Constant.SCRIPT_TYPE_SHELL_BATCH) {
                    String dbJarPath = scriptService.extractJarPath(dbScript.getScript());
                    String reqJarPath = scriptService.extractJarPath(req.getScript());
                    if (dbJarPath != null && !dbJarPath.equals(reqJarPath)) {
                        scriptService.deleteJar(dbScript);
                    }
                } else {
                    scriptService.deleteJar(dbScript);
                }
            }
        }
        req.setUpdateTime(now);
        Script script = new Script();
        BeanUtils.copyProperties(req, script);
        if (StringUtils.isBlank(req.getUser())) {
            script.setUser(sshConfig.getUser());
        }
        script = scriptService.save(script);
        if (req.getId() == null) {
            req.setId(script.getId());
        }
        return success(req);
    }

    @RequestMapping(value = "/delete.api", method = RequestMethod.POST)
    public Msg delete(@RequestParam String id) {
        Script script = scriptService.findById(id);
        if (script != null) {
            List<Scheduling> schedulings = schedulingService.findByQuery("scriptIds?" + script.getId());
            if (!schedulings.isEmpty()) {
                return failed("删除脚本前请先移除相关任务调度");
            }
            scriptService.delete(script);
        }
        return success();
    }

    @RequestMapping(value = "/execute.api", method = RequestMethod.POST)
    public Msg execute(@RequestBody DtoScript req) throws SchedulerException {
        LoginUser user = getCurrentUser();
        CmdRecord cmdRecord = CmdRecord.builder()
                .uid(user.getId())
                .scriptId(req.getId())
                .status(Constant.EXEC_STATUS_UNSTART)
                .agentId(req.getAgentId())
                .clusterId(req.getClusterId())
                .content(req.getScript())
                .timeout(req.getTimeout())
                .createTime(new Date())
                .build();
        cmdRecord = cmdRecordService.save(cmdRecord);
        CmdRecordRunner.build(cmdRecord);
        return success(cmdRecord);
    }

    /**
     * 检查脚本是否合法
     * @param req
     * @return
     */
    private String checkLegal(DtoScript req) {
        if (req.getId() == null) {
            //检查名称是否重复
            Script script = scriptService.findOneByQuery("name=" + req.getName());
            if (script != null) {
                return "名称重复";
            }
            if (req.getType() != Constant.SCRIPT_TYPE_SHELL_BATCH) {
                try {
                    agentService.getInstanceByClusterId(req.getClusterId(), false);
                } catch (IllegalStateException e) {
                    return "所选集群下暂无可用代理实例";
                }
                //检查yarn应用名称是否重复
                Set<String> queueAndApps = new HashSet<>();
                List<Script> scripts = scriptService.findByQuery("clusterId=" + req.getClusterId());
                scripts.forEach(item -> queueAndApps.add(item.getUser() + APP_APPEND_SYMBOL + item.getQueue() + APP_APPEND_SYMBOL + item.getApp()));
                dealUserAndQueue(queueAndApps, req);
                String queueAndApp = (req.getUser() != null ? req.getUser() : sshConfig.getUser()) + APP_APPEND_SYMBOL + req.getQueue() + APP_APPEND_SYMBOL + req.getApp();
                for (String tmp : queueAndApps) {
                    if (queueAndApp.equals(tmp)) {
                        return "YARN应用重复";
                    }
                }
            } else {
                try {
                    agentService.getInstanceByAgentId(req.getAgentId(), false);
                } catch (IllegalStateException e) {
                    return "所选代理下暂无可用实例";
                }
            }
        } else {
            Script script = scriptService.findById(req.getId());
            if (script == null) {
                return "脚本不存在或已被删除";
            }
            //更换名称的情况下，检查是否和其他任务名称冲突
            if (!req.getName().equals(script.getName())) {
                List<Script> scripts = scriptService.findByQuery("name=" + req.getName());
                for (Script info : scripts) {
                    if (!info.getId().equals(req.getId())) {
                        return "名称重复";
                    }
                }
            }
            if (req.getType() != Constant.SCRIPT_TYPE_SHELL_BATCH) {
                try {
                    agentService.getInstanceByClusterId(req.getClusterId(), false);
                } catch (IllegalStateException e) {
                    return "所选集群下暂无可用代理实例";
                }
                //检查yarn应用名称是否重复
                Set<String> queueAndApps = new HashSet<>();
                List<Script> scripts = new ArrayList<>();
                scriptService.findByQuery("clusterId=" + req.getClusterId()).forEach(item -> {
                    //排除自身
                    if (!req.getId().equals(item.getId())) {
                        scripts.add(item);
                    }
                });
                scripts.forEach(item -> queueAndApps.add(item.getUser() + APP_APPEND_SYMBOL + item.getQueue() + APP_APPEND_SYMBOL + item.getApp()));
                dealUserAndQueue(queueAndApps, req);
                String queueAndApp = (req.getUser() != null ? req.getUser() : sshConfig.getUser()) + APP_APPEND_SYMBOL + req.getQueue() + APP_APPEND_SYMBOL + req.getApp();
                for (String tmp : queueAndApps) {
                    if (queueAndApp.equals(tmp)) {
                        return "YARN应用重复";
                    }
                }
            } else {
                try {
                    agentService.getInstanceByAgentId(req.getAgentId(), false);
                } catch (IllegalStateException e) {
                    return "所选代理下暂无可用实例";
                }
            }
        }
        return null;
    }

    /**
     * 处理用户和队列
     * @param queueAndApps
     * @param req
     */
    private void dealUserAndQueue(Set<String> queueAndApps, DtoScript req) {
        req.setUser(null);
        String script = req.getScript();
        ClusterUser user = clusterUserService.findOneByQuery("uid=" + req.getUid() + ";clusterId=" + req.getClusterId());
        String queue = user.getQueue();
        String argQueue = req.getQueue();
        if (req.getType() == Constant.SCRIPT_TYPE_SPARK_STREAMING || req.getType() == Constant.SCRIPT_TYPE_SPARK_BATCH) {
            String proxyUser = user.getUser();
            if (StringUtils.isNotBlank(proxyUser)) {
                req.setUser(proxyUser);
            } else {
                proxyUser = sshConfig.getUser();
            }
            String legalQueue;
            if (argQueue != null) {
                if (!queue.contains(",")) {
                    legalQueue = queue;
                } else {
                    legalQueue = getLegalQueue(queueAndApps, proxyUser, queue, argQueue, req.getApp());
                }
                script = script.replaceAll("--queue [\\w-.,]+", "--queue " + legalQueue);
            } else {
                if (!queue.contains(",")) {
                    legalQueue = queue;
                } else {
                    legalQueue = getLegalQueue(queueAndApps, proxyUser, queue, null, req.getApp());
                }
                int pos = script.indexOf("--");
                String start = script.substring(0, pos - 1);
                String end = script.substring(pos);
                String conf = "--queue " + legalQueue;
                script = start + " " + conf + " " + end;
            }
            req.setQueue(legalQueue);
        } else {
            String proxyUser = sshConfig.getUser();
            Cluster cluster = clusterService.findById(req.getClusterId());
            if (cluster.getFlinkProxyUserEnabled() && StringUtils.isNotBlank(user.getUser())) {
                req.setUser(user.getUser());
                proxyUser = user.getUser();
            }
            String legalQueue;
            if (argQueue != null) {
                if (!queue.contains(",")) {
                    legalQueue = queue;
                } else {
                    legalQueue = getLegalQueue(queueAndApps, proxyUser, queue, argQueue, req.getApp());
                }
                script = script.replaceAll("-yqu [\\w-.,]+", "-yqu " + legalQueue);
            } else {
                if (!queue.contains(",")) {
                    legalQueue = queue;
                } else {
                    legalQueue = getLegalQueue(queueAndApps, proxyUser, queue, null, req.getApp());
                }
                int pos = script.indexOf("-");
                String start = script.substring(0, pos - 1);
                String end = script.substring(pos);
                String conf = "-yqu " + legalQueue;
                script = start + " " + conf + " " + end;
            }
            req.setQueue(legalQueue);
        }
        req.setScript(script);
    }

    /**
     * 获取合法队列
     * @param queueAndApps
     * @param proxyUser
     * @param queue
     * @param argQueue
     * @param argApp
     * @return
     */
    private String getLegalQueue(Set<String> queueAndApps, String proxyUser, String queue, String argQueue, String argApp) {
        String legalQueue = null;
        String [] queueArr = queue.split(",");
        if (argQueue != null) {
            for (String tmpQueue : queueArr) {
                if (tmpQueue.equals(argQueue)) {
                    legalQueue = argQueue;
                    break;
                }
            }
        }
        if (legalQueue == null) {
            List<String> legalQueues = new ArrayList<>();
            for (String q : queueArr) {
                String tmp = proxyUser + APP_APPEND_SYMBOL + q + APP_APPEND_SYMBOL + argApp;
                boolean exist = false;
                for (String qAa : queueAndApps) {
                    if (tmp.equals(qAa)) {
                        //已存在
                        exist = true;
                        break;
                    }
                }
                if (!exist) {
                    legalQueues.add(q);
                }
            }
            if (!legalQueues.isEmpty()) {
                queueArr = new String[legalQueues.size()];
                queueArr = legalQueues.toArray(queueArr);
            }
            Random random = new Random();
            legalQueue = queueArr[random.nextInt(queueArr.length)];
        }
        return legalQueue;
    }

    private Map<String, Integer> calResource(int type, String script) {
        Map<String, Integer> result = new HashMap<>();
        Map<String, String> spark = new HashMap<>();
        spark.put("--driver-memory", "512M");
        spark.put("--driver-cores", "1");
        spark.put("--executor-memory", "1024M");
        spark.put("--num-executors", "1");
        spark.put("--executor-cores", "1");
        spark.put("spark.yarn.executor.memoryOverhead", "-");
        Map<String, String> flink = new HashMap<>();
        flink.put("-yjm", "512M");
        flink.put("-ytm", "1024M");
        flink.put("-yn", "1");
        flink.put("-ys", "1");
        String [] tokens = script.split(" ");
        for (int i = 0; i < tokens.length; i ++) {
            String token = tokens[i];
            if (type == Constant.SCRIPT_TYPE_SPARK_STREAMING || type == Constant.SCRIPT_TYPE_SPARK_BATCH) {
                if (token.startsWith("spark.yarn.executor.memoryOverhead")) {
                    spark.put("spark.yarn.executor.memoryOverhead", token.split("=")[1]);
                } else if (spark.containsKey(token)) {
                    spark.put(token, tokens[i + 1]);
                }
            } else {
                if (flink.containsKey(token)) {
                    flink.put(token, tokens[i + 1]);
                }
            }
        }
        if (type == Constant.SCRIPT_TYPE_SPARK_STREAMING || type == Constant.SCRIPT_TYPE_SPARK_BATCH) {
            String driverMemoryStr = spark.get("--driver-memory").toUpperCase();
            int driverMemory = parseMemory(driverMemoryStr);
            String executorMemoryStr = spark.get("--executor-memory");
            int executorMemory = parseMemory(executorMemoryStr);
            int numExecutors = Integer.parseInt(spark.get("--num-executors"));
            String memoryOverheadStr = spark.get("spark.yarn.executor.memoryOverhead");
            if (!"-".equals(memoryOverheadStr)) {
                int memoryOverhead = parseMemory(memoryOverheadStr);
                result.put("totalMemory", numExecutors * (executorMemory + memoryOverhead) + (driverMemory + memoryOverhead));
            } else {
                result.put("totalMemory", (int)(numExecutors * (executorMemory + Math.max(executorMemory * 0.1, 384)) + (driverMemory + Math.max(driverMemory * 0.1, 384))));
            }
            int driverCores = Integer.parseInt(spark.get("--driver-cores"));
            int executorCores = Integer.parseInt(spark.get("--executor-cores"));
            result.put("totalCores", numExecutors * executorCores + driverCores);
        } else {
            String yjmStr = flink.get("-yjm").toUpperCase();
            int yjm = parseMemory(yjmStr);
            String ytmStr = flink.get("-ytm").toUpperCase();
            int ytm = parseMemory(ytmStr);
            int yn = Integer.parseInt(flink.get("-yn"));
            result.put("totalMemory", yn * ytm + yjm);
            int ys = Integer.parseInt(flink.get("-ys"));
            result.put("totalCores", yn * ys + 1);
        }
        return result;
    }

    private int parseMemory(String present) {
        present = present.toUpperCase();
        int val;
        if (present.contains("M")) {
            val = Integer.parseInt(present.substring(0, present.indexOf('M')));
        } else if (present.contains("G")) {
            val = Integer.parseInt(present.substring(0, present.indexOf('G'))) * 1024;
        } else {
            val = Integer.parseInt(present);
        }
        return val;
    }

    /**
     * 补充必要参数
     * @param req
     */
    private void appendNecessaryArgs(DtoScript req) {
        String user = req.getUser();
        String script = req.getScript();
        if (req.getType() == Constant.SCRIPT_TYPE_SPARK_STREAMING || req.getType() == Constant.SCRIPT_TYPE_SPARK_BATCH) {
            if (!script.contains("spark.yarn.submit.waitAppCompletion")) {
                int pos = script.indexOf("--");
                String start = script.substring(0, pos - 1);
                String end = script.substring(pos);
                String conf = "--conf spark.yarn.submit.waitAppCompletion=false";
                script = start + " " + conf + " " + end;
            }
            if (StringUtils.isNotBlank(user)) {
                if (!script.contains("--proxy-user ")) {
                    int pos = script.indexOf("--");
                    String start = script.substring(0, pos - 1);
                    String end = script.substring(pos);
                    String conf = "--proxy-user " + user;
                    script = start + " " + conf + " " + end;
                } else {
                    script = script.replaceAll("--proxy-user [\\w-.,]+", "--proxy-user " + user);
                }
            }
        } else {
            if (!script.contains(" -d ")) {
                int pos = script.indexOf(" -");
                String start = script.substring(0, pos);
                String end = script.substring(pos + 1);
                String conf = "-d";
                script = start + " " + conf + " " + end;
            }
            if (StringUtils.isNotBlank(user)) {
                if (!script.contains("-yD ypu=")) {
                    int pos = script.indexOf(" -");
                    String start = script.substring(0, pos);
                    String end = script.substring(pos + 1);
                    String conf = "-yD ypu=" + user;
                    script = start + " " + conf + " " + end;
                } else {
                    script = script.replaceAll("-yD ypu=[\\w-.,]+", "-yD ypu=" + user);
                }
            }
        }
        req.setScript(script);
    }

    /**
     * 更新集群或队列检查应用是否正在运行
     * @param dbScript
     * @param req
     * @return
     */
    private boolean checkYarnAppAliveIfChangeClusterOrQueue(Script dbScript, DtoScript req) {
        String queueOld = dbScript.getQueue();
        if (req.getType() != Constant.SCRIPT_TYPE_SHELL_BATCH) {
            String queueReq = req.getQueue();
            if (!dbScript.getClusterId().equals(req.getClusterId()) || !queueOld.equals(queueReq)) {
                Cluster clusterOld = clusterService.findById(dbScript.getClusterId());
                return YarnApiUtils.getActiveApp(clusterOld.getYarnUrl(), dbScript.getUser(), queueOld, dbScript.getApp(), 3) != null;
            }
            return false;
        } else {
            Cluster clusterOld = clusterService.findById(dbScript.getClusterId());
            return YarnApiUtils.getActiveApp(clusterOld.getYarnUrl(), dbScript.getUser(), queueOld, dbScript.getApp(), 3) != null;
        }
    }

}
