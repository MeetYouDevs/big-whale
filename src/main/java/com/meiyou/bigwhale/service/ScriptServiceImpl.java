package com.meiyou.bigwhale.service;

import com.meiyou.bigwhale.common.Constant;
import com.meiyou.bigwhale.config.SshConfig;
import com.meiyou.bigwhale.config.YarnConfig;
import com.meiyou.bigwhale.data.service.AbstractMysqlPagingAndSortingQueryService;
import com.meiyou.bigwhale.dto.DtoScript;
import com.meiyou.bigwhale.entity.*;
import com.meiyou.bigwhale.entity.auth.User;
import com.meiyou.bigwhale.job.MonitorJob;
import com.meiyou.bigwhale.job.ScriptHistoryShellRunnerJob;
import com.meiyou.bigwhale.service.auth.UserService;
import com.meiyou.bigwhale.util.SchedulerUtils;
import com.meiyou.bigwhale.util.WebHdfsUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ScriptServiceImpl extends AbstractMysqlPagingAndSortingQueryService<Script, Integer> implements ScriptService {

    private static final Pattern DATE_PATTERN = Pattern.compile("(\\$\\{now(\\s*([-+])\\s*(\\d+)([dhms]))*(@([A-Za-z0-9\\s-':+.]+))*})+");

    @Autowired
    private MonitorService monitorService;
    @Autowired
    private ScriptHistoryService scriptHistoryService;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private UserService userService;
    @Autowired
    private AgentService agentService;
    @Autowired
    private ClusterUserService clusterUserService;
    @Autowired
    private SshConfig sshConfig;
    @Autowired
    private YarnConfig yarnConfig;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(Script entity) {
        //删除jar包
        if (entity.isYarn()) {
            deleteJar(entity);
        }
        if (entity.getMonitorId() != null) {
            monitorService.deleteById(entity.getMonitorId());
        }
        scriptHistoryService.deleteByQuery("scriptId=" + entity.getId());
        super.delete(entity);
    }

    @Override
    public Page<Script> fuzzyPage(DtoScript req) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Script> criteriaQuery = builder.createQuery(clazz);
        Root<Script> root = criteriaQuery.from(clazz);
        CriteriaQuery<Long> countCriteriaQuery = builder.createQuery(Long.class);
        Root<Script> countRoot = countCriteriaQuery.from(clazz);
        countCriteriaQuery.select(builder.count(countRoot));
        this.predicate(builder, criteriaQuery, root, req);
        this.predicate(builder, countCriteriaQuery, countRoot, req);
        long totalCount = entityManager.createQuery(countCriteriaQuery).getSingleResult();
        int pageNo = req.pageNo - 1;
        int pageSize = req.pageSize;
        TypedQuery<Script> typedQuery = entityManager.createQuery(criteriaQuery);
        typedQuery.setFirstResult(pageNo * pageSize);
        typedQuery.setMaxResults(pageSize);
        List<Script> entities = typedQuery.getResultList();
        return new PageImpl<>(entities, new PageRequest(pageNo, pageSize), totalCount);
    }

    @Override
    public String validate(DtoScript req) {
        String msg = checkLegal(req);
        if (msg != null) {
            return msg;
        }
        //应用内存资源参数检查和补充必要参数
        if (req.isYarn()) {
            if (yarnConfig.getAppMemoryThreshold() > 0 && !yarnConfig.getAppWhiteList().contains(req.getApp())) {
                try {
                    int totalMemory = calResource(req.getType(), req.getContent()).get("totalMemory");
                    if (totalMemory > yarnConfig.getAppMemoryThreshold()) {
                        //超过阀值
                        return "内存参数配置达到大内存应用标准【" + yarnConfig.getAppMemoryThreshold() + "MB】 ！请调小内存参数或联系管理员升级为大内存应用。";
                    }
                } catch (NumberFormatException e) {
                    return "内存参数请向上取整";
                }
            }
            appendNecessaryArgs(req);
        }
        if (req.getId() != null) {
            Script dbScript = findById(req.getId());
            if (dbScript.isYarn()) {
                //更换集群或队列时检查应用是否正在运行
                if (checkNeedKillYarnAppIfChangeClusterOrQueue(dbScript, req)) {
                    return "更换集群或队列前请先关闭正在运行的应用";
                }
                //检查程序包是否变更
                String dbJarPath = extractJarPath(dbScript.getContent());
                String reqJarPath = extractJarPath(req.getContent());
                if (!dbJarPath.equals(reqJarPath)) {
                    deleteJar(dbScript);
                }
            }
        }
        if (StringUtils.isBlank(req.getUser())) {
            req.setUser(sshConfig.getUser());
        }
        return null;
    }

    @Override
    public void update(Script entity, Monitor monitorEntity) {
        Monitor monitor = monitorService.save(monitorEntity);
        if (entity.getMonitorId() == null) {
            entity.setMonitorId(monitor.getId());
        }
        super.save(entity);
        SchedulerUtils.deleteJob(monitor.getId(), Constant.JobGroup.MONITOR);
        if (monitor.getEnabled()) {
            MonitorJob.build(monitor);
        }
    }

    @Override
    public boolean execute(Script script, Monitor monitor) {
        ScriptHistory scriptHistory = generateHistory(script, monitor, null, null, null);
        if (scriptHistory != null) {
            ScriptHistoryShellRunnerJob.build(scriptHistory);
        }
        return scriptHistory != null;
    }

    @Override
    public ScriptHistory generateHistory(Script script) {
        return generateHistory(script, null, null, null, null);
    }

    @Override
    public ScriptHistory generateHistory(Script script, ScheduleSnapshot scheduleSnapshot, String scheduleInstanceId, int generateStatus) {
        return generateHistory(script, null, scheduleSnapshot, scheduleInstanceId, generateStatus);
    }

    @Override
    public String extractJarPath(String content) {
        String [] tokens = content.split(" ");
        int jarIndex = -1;
        for (int i = 0; i < tokens.length; i ++) {
            String token = tokens[i];
            if (token.contains(".jar") || token.contains(".py")) {
                if (!"--jars".equals(tokens[i - 1]) && !"-j".equals(tokens[i - 1]) && !"--jar".equals(tokens[i - 1])) {
                    jarIndex = i;
                    break;
                }
            }
        }
        if (jarIndex != -1) {
            return tokens[jarIndex];
        }
        return null;
    }

    @Override
    public void deleteJar(Script entity) {
        String jarPath = extractJarPath(entity.getContent());
        if (jarPath != null) {
            //检查是否还被引用
            boolean used = false;
            for (Script item : findByQuery("createBy=" + entity.getCreateBy() + ";id!=" + entity.getId())) {
                if (jarPath.equals(extractJarPath(item.getContent()))) {
                    used = true;
                    break;
                }
            }
            if (!used) {
                for (Cluster cluster : clusterService.findAll()) {
                    if (jarPath.startsWith(cluster.getFsDefaultFs())) {
                        String fsDefaultFs = cluster.getFsDefaultFs();
                        if (fsDefaultFs.endsWith("/")) {
                            fsDefaultFs = fsDefaultFs.substring(0, fsDefaultFs.length() - 1);
                        }
                        jarPath = jarPath.substring(fsDefaultFs.length());
                        User user = userService.findById(entity.getCreateBy());
                        WebHdfsUtils.delete(cluster.getFsWebhdfs(), jarPath, user.getUsername());
                        break;
                    }
                }
            }
        }
    }

    /**
     * 拼接查询表达式
     * @param builder
     * @param criteriaQuery
     * @param root
     * @param req
     */
    private void predicate(CriteriaBuilder builder, CriteriaQuery<?> criteriaQuery, Root<Script> root, DtoScript req) {
        List<Predicate> orPredicates = new ArrayList<>();
        if (StringUtils.isNotBlank(req.getText())) {
            Predicate predicate1 = builder.conjunction();
            predicate1.getExpressions().add(builder.like(root.get("name"), "%" + req.getText() + "%"));
            orPredicates.add(predicate1);
            Predicate predicate2 = builder.conjunction();
            predicate2.getExpressions().add(builder.like(root.get("description"), "%" + req.getText() + "%"));
            orPredicates.add(predicate2);
            Predicate predicate3 = builder.conjunction();
            predicate3.getExpressions().add(builder.like(root.get("content"), "%" + req.getText() + "%"));
            orPredicates.add(predicate3);
        }
        Predicate andPredicate = builder.conjunction();
        andPredicate.getExpressions().add(root.get("type").in(Constant.ScriptType.SPARK_STREAM, Constant.ScriptType.FLINK_STREAM));
        if (req.getMonitorEnabled() != null) {
            andPredicate.getExpressions().add(builder.equal(root.get("monitorEnabled"), req.getMonitorEnabled()));
        }
        if (req.getCreateBy() != null) {
            andPredicate.getExpressions().add(builder.equal(root.get("createBy"), req.getCreateBy()));
        }
        if (!orPredicates.isEmpty()) {
            Predicate orPredicate = builder.or(orPredicates.toArray(new Predicate[0]));
            criteriaQuery.where(andPredicate, orPredicate);
        } else {
            criteriaQuery.where(andPredicate);
        }
    }

    /**
     * 检查脚本是否合法
     * @param req
     * @return
     */
    private String checkLegal(DtoScript req) {
        if (req.getId() == null) {
            if (req.isYarn()) {
                try {
                    agentService.getInstanceByClusterId(req.getClusterId(), false);
                } catch (IllegalStateException e) {
                    return "所选集群下暂无可用代理实例";
                }
                //检查yarn应用名称是否重复
                Set<String> queueAndApps = new HashSet<>();
                List<Script> scripts = findByQuery("clusterId=" + req.getClusterId());
                scripts.forEach(script -> queueAndApps.add(script.getUser() + "$" + script.getQueue() + "$" + script.getApp()));
                dealUserAndQueue(queueAndApps, req);
                String queueAndApp = (req.getUser() != null ? req.getUser() : sshConfig.getUser()) + "$" + req.getQueue() + "$" + req.getApp();
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
            if (req.isYarn()) {
                try {
                    agentService.getInstanceByClusterId(req.getClusterId(), false);
                } catch (IllegalStateException e) {
                    return "所选集群下暂无可用代理实例";
                }
                //检查yarn应用名称是否重复
                Set<String> queueAndApps = new HashSet<>();
                List<Script> scripts = findByQuery("clusterId=" + req.getClusterId() + ";id!=" + req.getId());
                scripts.forEach(script -> queueAndApps.add(script.getUser() + "$" + script.getQueue() + "$" + script.getApp()));
                dealUserAndQueue(queueAndApps, req);
                String queueAndApp = (req.getUser() != null ? req.getUser() : sshConfig.getUser()) + "$" + req.getQueue() + "$" + req.getApp();
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
        String content = req.getContent();
        ClusterUser user = clusterUserService.findOneByQuery("clusterId=" + req.getClusterId() + ";userId=" + req.getCreateBy());
        String queue = user.getQueue();
        String argQueue = req.getQueue();
        if (Constant.ScriptType.SPARK_BATCH.equals(req.getType()) || Constant.ScriptType.SPARK_STREAM.equals(req.getType())) {
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
                content = content.replaceAll("--queue [\\w-.,]+", "--queue " + legalQueue);
            } else {
                if (!queue.contains(",")) {
                    legalQueue = queue;
                } else {
                    legalQueue = getLegalQueue(queueAndApps, proxyUser, queue, null, req.getApp());
                }
                int pos = content.indexOf("--");
                String start = content.substring(0, pos - 1);
                String end = content.substring(pos);
                String conf = "--queue " + legalQueue;
                content = start + " " + conf + " " + end;
            }
            req.setQueue(legalQueue);
        } else {
            String proxyUser = sshConfig.getUser();
            Cluster cluster = clusterService.findById(req.getClusterId());
            if (cluster.getFlinkProxyUserEnabled() && StringUtils.isNotBlank(user.getUser())) {
                // TODO 特殊处理
                if (content.startsWith("flink19")) {
                    req.setUser(user.getUser());
                    proxyUser = user.getUser();
                }
            }
            String legalQueue;
            if (argQueue != null) {
                if (!queue.contains(",")) {
                    legalQueue = queue;
                } else {
                    legalQueue = getLegalQueue(queueAndApps, proxyUser, queue, argQueue, req.getApp());
                }
                content = content.replaceAll("-yqu [\\w-.,]+", "-yqu " + legalQueue);
            } else {
                if (!queue.contains(",")) {
                    legalQueue = queue;
                } else {
                    legalQueue = getLegalQueue(queueAndApps, proxyUser, queue, null, req.getApp());
                }
                int pos = content.indexOf("-");
                String start = content.substring(0, pos - 1);
                String end = content.substring(pos);
                String conf = "-yqu " + legalQueue;
                content = start + " " + conf + " " + end;
            }
            req.setQueue(legalQueue);
        }
        req.setContent(content);
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
                String tmp = proxyUser + "$" + q + "$" + argApp;
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

    private Map<String, Integer> calResource(String type, String content) {
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
        String [] tokens = content.split(" ");
        for (int i = 0; i < tokens.length; i ++) {
            String token = tokens[i];
            if (Constant.ScriptType.SPARK_BATCH.equals(type) || Constant.ScriptType.SPARK_STREAM.equals(type)) {
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
        if (Constant.ScriptType.SPARK_BATCH.equals(type) || Constant.ScriptType.SPARK_STREAM.equals(type)) {
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
        String content = req.getContent();
        if (Constant.ScriptType.SPARK_BATCH.equals(req.getType()) || Constant.ScriptType.SPARK_STREAM.equals(req.getType())) {
            if (!content.contains("spark.yarn.submit.waitAppCompletion")) {
                int pos = content.indexOf("--");
                String start = content.substring(0, pos - 1);
                String end = content.substring(pos);
                String conf = "--conf spark.yarn.submit.waitAppCompletion=false";
                content = start + " " + conf + " " + end;
            }
            if (StringUtils.isNotBlank(user)) {
                if (!content.contains("--proxy-user ")) {
                    int pos = content.indexOf("--");
                    String start = content.substring(0, pos - 1);
                    String end = content.substring(pos);
                    String conf = "--proxy-user " + user;
                    content = start + " " + conf + " " + end;
                } else {
                    content = content.replaceAll("--proxy-user [\\w-.,]+", "--proxy-user " + user);
                }
            }
        } else {
            if (!content.contains(" -d ")) {
                int pos = content.indexOf(" -");
                String start = content.substring(0, pos);
                String end = content.substring(pos + 1);
                String conf = "-d";
                content = start + " " + conf + " " + end;
            }
            if (StringUtils.isNotBlank(user)) {
                if (!content.contains("-yD ypu=")) {
                    int pos = content.indexOf(" -");
                    String start = content.substring(0, pos);
                    String end = content.substring(pos + 1);
                    String conf = "-yD ypu=" + user;
                    content = start + " " + conf + " " + end;
                } else {
                    content = content.replaceAll("-yD ypu=[\\w-.,]+", "-yD ypu=" + user);
                }
            }
        }
        req.setContent(content);
    }

    /**
     * 更新集群或队列检查应用是否正在运行
     * @param dbScript
     * @param req
     * @return
     */
    private boolean checkNeedKillYarnAppIfChangeClusterOrQueue(Script dbScript, DtoScript req) {
        String queueOld = dbScript.getQueue();
        String queueReq = req.getQueue();
        if (!dbScript.getClusterId().equals(req.getClusterId()) || !queueOld.equals(queueReq)) {
            // 只判断最近的一个任务状态，非最近的任务如果还在运行jobFinalStatus将会被更新为UNKNOWN
            ScriptHistory scriptHistory = scriptHistoryService.findOneByQuery("scriptId=" + dbScript.getId(), new Sort(Sort.Direction.DESC, "createTime", "id"));
            return scriptHistory != null && scriptHistory.isRunning();
        }
        return false;
    }

    /**
     * @param script
     * @param monitor
     * @param scheduleSnapshot
     * @param scheduleInstanceId
     * @param generateStatus
     * @return 0 需确认 1 确认 2补数
     */
    private ScriptHistory generateHistory(Script script, Monitor monitor, ScheduleSnapshot scheduleSnapshot, String scheduleInstanceId, Integer generateStatus) {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String suffix;
        if (scheduleSnapshot != null) {
            suffix = scheduleInstanceId;
        } else {
            suffix = dateFormat.format(new Date());
        }
        Date date;
        try {
            date = dateFormat.parse(suffix);
        } catch (ParseException e) {
            throw new RuntimeException("Error date format: " + suffix);
        }
        ScriptHistory scriptHistory;
        if (scheduleSnapshot != null && generateStatus == 1) {
            scriptHistory = scriptHistoryService.findOneByQuery("scheduleId=" + scheduleSnapshot.getScheduleId() +
                    ";scheduleTopNodeId=" + script.getScheduleTopNodeId() +
                    ";scheduleInstanceId=" + scheduleInstanceId +
                    ";state=" + Constant.JobState.UN_CONFIRMED_);
        } else {
            scriptHistory = ScriptHistory.builder()
                    .scriptId(script.getId())
                    .scriptType(script.getType())
                    .agentId(script.getAgentId())
                    .clusterId(script.getClusterId())
                    .timeout(script.getTimeout())
                    .createTime(date)
                    .createBy(script.getCreateBy())
                    .build();
        }
        if (scheduleSnapshot != null) {
            scriptHistory.setScheduleId(scheduleSnapshot.getScheduleId());
            scriptHistory.setScheduleTopNodeId(script.getScheduleTopNodeId());
            scriptHistory.setScheduleSnapshotId(scheduleSnapshot.getId());
            scriptHistory.setScheduleInstanceId(scheduleInstanceId);
            if (generateStatus == 2) {
                scriptHistory.setScheduleHistoryMode(Constant.HistoryMode.SUPPLEMENT);
                Date now = new Date();
                scriptHistory.setScheduleHistoryTime(date.compareTo(now) <= 0 ? now : date);
            }
        }
        if (monitor != null) {
            scriptHistory.setMonitorId(monitor.getId());
        }
        String command;
        String content = parseTimeArgs(script.getContent(), date);
        String yarnAppSuffix;
        if (script.getId() != null) {
            yarnAppSuffix = ".bw_instance_" + (script.isBatch() ? "b" : "s") + suffix;
        } else {
            yarnAppSuffix = ".bw_test_instance_" + (script.isBatch() ? "b" : "s") + suffix;
        }
        if (Constant.ScriptType.SPARK_BATCH.equals(script.getType()) || Constant.ScriptType.SPARK_STREAM.equals(script.getType())) {
            command = content.replace("--name " + script.getApp(), "--name " + script.getApp() + yarnAppSuffix);
        } else if (Constant.ScriptType.FLINK_BATCH.equals(script.getType()) || Constant.ScriptType.FLINK_STREAM.equals(script.getType())) {
            command = content.replace("-ynm " + script.getApp(), "-ynm " + script.getApp() + yarnAppSuffix);
        } else if (Constant.ScriptType.PYTHON.equals(script.getType())) {
            String dir = "/tmp/trochilus/data/code/" + Math.abs(script.getName().hashCode());
            String path = dir + "/" + suffix + ".py";
            command = "mkdir -p " + dir;
            command += " && ";
            command += "touch " + path;
            command += " && ";
            command += "echo \"" + content + "\" >> " + path;
            command += " && ";
            command += "python ";
            command += path;
        } else {
            command = content;
        }
        scriptHistory.setContent(command);
        if (script.isYarn() && scriptHistory.getScriptId() == null) {
            scriptHistory.setOutputs("proxy user: " + script.getUser() + "\n");
        }
        if (scheduleSnapshot == null) {
            scriptHistory.updateState(Constant.JobState.INITED);
        } else {
            if (generateStatus == 0 || generateStatus == 2) {
                scriptHistory.updateState(Constant.JobState.UN_CONFIRMED_);
            } else {
                scriptHistory.updateState(Constant.JobState.WAITING_PARENT_);
            }
        }
        return scriptHistoryService.save(scriptHistory);
    }

    private String parseTimeArgs(String raw, Date date) {
        Matcher matcher = DATE_PATTERN.matcher(raw);
        while (matcher.find()) {
            if (matcher.group(0) != null) {
                Date replace = date;
                if (matcher.group(2) != null) {
                    int op = "+".equals(matcher.group(3)) ? 1 : -1;
                    int amount = Integer.parseInt(matcher.group(4));
                    switch (matcher.group(5)) {
                        case "d":
                            replace = DateUtils.addDays(date, op * amount);
                            break;
                        case "h":
                            replace = DateUtils.addHours(date, op * amount);
                            break;
                        case "m":
                            replace = DateUtils.addMinutes(date, op * amount);
                            break;
                        case "s":
                            replace = DateUtils.addSeconds(date, op * amount);
                            break;
                        default:
                            break;
                    }
                }
                if (matcher.group(6) != null) {
                    try {
                        raw = raw.replace(matcher.group(1), new SimpleDateFormat(matcher.group(7)).format(replace));
                    } catch (IllegalArgumentException e) {
                        // throw new IllegalArgumentException("Illegal pattern character '" + matcher.group(7) + "'");
                        return raw;
                    }
                } else {
                    raw = raw.replace(matcher.group(1), new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(replace));
                }
            }
        }
        return raw;
    }

}
