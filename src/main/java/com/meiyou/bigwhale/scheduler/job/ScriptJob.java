package com.meiyou.bigwhale.scheduler.job;

import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import com.meiyou.bigwhale.common.Constant;
import com.meiyou.bigwhale.common.pojo.HttpYarnApp;
import com.meiyou.bigwhale.config.SshConfig;
import com.meiyou.bigwhale.entity.Cluster;
import com.meiyou.bigwhale.entity.ScriptHistory;
import com.meiyou.bigwhale.scheduler.AbstractRetryable;
import com.meiyou.bigwhale.service.AgentService;
import com.meiyou.bigwhale.service.ClusterService;
import com.meiyou.bigwhale.service.ScriptHistoryService;
import com.meiyou.bigwhale.util.SchedulerUtils;
import com.meiyou.bigwhale.util.YarnApiUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.InterruptableJob;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Suxy
 * @date 2021/2/6
 * @description file description
 */
@DisallowConcurrentExecution
public class ScriptJob extends AbstractRetryable implements InterruptableJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptJob.class);

    private static final Pattern YARN_PATTERN = Pattern.compile("application_\\d+_\\d+");
    private static final Pattern KILL_PATTERN = Pattern.compile("time mark: (\\d+)");

    private JobExecutionContext context;
    private Thread thread;
    private ScriptHistory scriptHistory;
    private volatile boolean commandFinish = false;
    private volatile boolean interrupted = false;
    private Connection conn;

    @Autowired
    private ScriptHistoryService scriptHistoryService;
    @Autowired
    private AgentService agentService;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private SshConfig sshConfig;

    @Override
    public void interrupt() {
        if (!commandFinish && !interrupted) {
            kill();
            interrupted = true;
            thread.interrupt();
        }
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        context = jobExecutionContext;
        thread = Thread.currentThread();
        Integer scriptHistoryId = Integer.parseInt(jobExecutionContext.getJobDetail().getKey().getName());
        scriptHistory = scriptHistoryService.findById(scriptHistoryId);
        if ((System.currentTimeMillis() - scriptHistory.getSubmitTime().getTime()) > (scriptHistory.getTimeout() * 60 * 1000)) {
            scriptHistory.updateState(Constant.JobState.TIMEOUT);
            scriptHistory.setFinishTime(new Date());
            scriptHistoryService.save(scriptHistory);
            interrupted = true;
            return;
        }
        String command = scriptHistory.getContent();
        try {
           if (Constant.ScriptType.SHELL.equals(scriptHistory.getScriptType())) {
               runCommonShell(command);
           } else {
               runYarnShell(command);
           }
        } catch (Exception e) {
            if (interrupted) {
                dealInterrupted();
                return;
            }
            LOGGER.error(e.getMessage(), e);
            scriptHistory.updateState(Constant.JobState.FAILED);
            scriptHistory.setErrors(e.getMessage());
            scriptHistory.setFinishTime(new Date());
            scriptHistoryService.save(scriptHistory);
            //重试
            retryCurrentNode(scriptHistory, Constant.ErrorType.FAILED);
        }
    }

    private void runYarnShell(String command) throws Exception {
        Session session = null;
        try {
            String instance = agentService.getInstanceByClusterId(scriptHistory.getClusterId(), true);
            if (instance.contains(":")) {
                String [] arr = instance.split(":");
                conn = new Connection(arr[0], Integer.parseInt(arr[1]));
            } else {
                conn = new Connection(instance);
            }
            conn.connect(null, sshConfig.getConnectTimeout(), 30000);
            conn.authenticateWithPassword(sshConfig.getUser(), sshConfig.getPassword());
            session = conn.openSession();
            session.execCommand("echo time mark: $(date +%s) && " + command);
            if (!interrupted) {
                //并发执行读取
                readOutput(session);
            }
            //主逻辑执行结束，就不再需要执行interrupt()
            commandFinish = true;
            if (!interrupted) {
                //等待指令执行完退出
                session.waitForCondition(ChannelCondition.EXIT_STATUS, 1000);
                //取得指令执行结束后的状态
                int ret = session.getExitStatus();
                if (ret == 0) {
                    scriptHistory.updateState(Constant.JobState.SUBMITTED);
                    Matcher matcher = YARN_PATTERN.matcher(scriptHistory.getOutputs());
                    if (matcher.find()) {
                        String id = matcher.group();
                        scriptHistory.setJobId(id);
                        String yarnUrl = clusterService.findById(scriptHistory.getClusterId()).getYarnUrl();
                        scriptHistory.setJobUrl(yarnUrl + "/proxy/" + id + "/");
                    }
                    scriptHistory.setJobFinalStatus("UNDEFINED");
                } else {
                    scriptHistory.updateState(Constant.JobState.FAILED);
                    scriptHistory.setFinishTime(new Date());
                }
                scriptHistoryService.save(scriptHistory);
                if (ret != 0) {
                    //重试
                    retryCurrentNode(scriptHistory, Constant.ErrorType.FAILED);
                }
            } else {
                dealInterrupted();
            }
        } finally {
            if (session != null) {
                session.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
    }

    private void runCommonShell(String command) throws Exception {
        Session session = null;
        try {
            String instance = agentService.getInstanceByAgentId(scriptHistory.getAgentId(), true);
            if (instance.contains(":")) {
                String [] arr = instance.split(":");
                conn = new Connection(arr[0], Integer.parseInt(arr[1]));
            } else {
                conn = new Connection(instance);
            }
            conn.connect(null, sshConfig.getConnectTimeout(), 30000);
            conn.authenticateWithPassword(sshConfig.getUser(), sshConfig.getPassword());
            session = conn.openSession();
            session.execCommand("echo time mark: $(date +%s) && " + command);
            if (!interrupted) {
                scriptHistory.updateState(Constant.JobState.SUBMITTED);
                scriptHistory.updateState(Constant.JobState.ACCEPTED);
                scriptHistory.updateState(Constant.JobState.RUNNING);
                scriptHistory.setStartTime(new Date());
                scriptHistoryService.save(scriptHistory);
                //并发执行读取
                readOutput(session);
            }
            //主逻辑执行结束，就不再需要执行interrupt()
            commandFinish = true;
            if (!interrupted) {
                //等待指令执行完退出
                session.waitForCondition(ChannelCondition.EXIT_STATUS, 1000);
                //取得指令执行结束后的状态
                int ret = session.getExitStatus();
                if (ret == 0) {
                    scriptHistory.updateState(Constant.JobState.SUCCEEDED);
                } else {
                    scriptHistory.updateState(Constant.JobState.FAILED);
                }
                scriptHistory.setFinishTime(new Date());
                scriptHistoryService.save(scriptHistory);
                if (ret != 0) {
                    //重试
                    retryCurrentNode(scriptHistory, Constant.ErrorType.FAILED);
                }
            } else {
                dealInterrupted();
            }
        } finally {
            if (session != null) {
                session.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
    }

    private void readOutput(Session session) {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch countDownLatch = new CountDownLatch(2);
            final InputStream stdOut = new StreamGobbler(session.getStdout());
            final InputStream stdErr = new StreamGobbler(session.getStderr());
            executorService.execute(() -> {
                try {
                    readContent(true, stdOut);
                } finally {
                    countDownLatch.countDown();
                }
            });
            executorService.execute(() -> {
                try {
                    readContent(false, stdErr);
                } finally {
                    countDownLatch.countDown();
                }
            });
            countDownLatch.await();
        } catch (InterruptedException e) {

        } finally {
            executorService.shutdownNow();
        }
    }

    private void readContent(boolean stdout, InputStream in) {
        String prefix = scriptHistory.getOutputs() != null ? scriptHistory.getOutputs() : "";
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) != -1) {
                if (out.size() < 61440) {
                    out.write(buffer, 0, length);
                    String tmp = out.toString("UTF-8");
                    if (stdout) {
                        scriptHistory.setOutputs(prefix + tmp);
                    } else {
                        scriptHistory.setErrors(tmp);
                    }
                    scriptHistoryService.save(scriptHistory);
                }
            }
            if (out.size() > 0) {
                String content = out.toString("UTF-8");
                if (stdout) {
                    scriptHistory.setOutputs(prefix + content);
                } else {
                    scriptHistory.setErrors(content);
                }
            }
        } catch (IOException e) {

        } finally {
            try {
                in.close();
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    private void dealInterrupted() {
        Object timeout = context.getMergedJobDataMap().get("timeout");
        if (timeout != null) {
            if (scriptHistory.getClusterId() != null && scriptHistory.getState().equals(Constant.JobState.SUBMITTING)) {
                Cluster cluster = clusterService.findById(scriptHistory.getClusterId());
                String [] jobParams = scriptHistory.getJobParams().split(";");
                HttpYarnApp httpYarnApp = YarnApiUtils.getActiveApp(cluster.getYarnUrl(), jobParams[0], jobParams[1], jobParams[2], 3);
                if (httpYarnApp != null) {
                    scriptHistory.updateState(Constant.JobState.SUBMITTED);
                    scriptHistory.setJobFinalStatus("UNDEFINED");
                    scriptHistoryService.save(scriptHistory);
                    return;
                }
            }
            scriptHistory.updateState(Constant.JobState.TIMEOUT);
        } else {
            scriptHistory.updateState(Constant.JobState.KILLED);
        }
        scriptHistory.setFinishTime(new Date());
        scriptHistoryService.save(scriptHistory);
        // 手动kill不重试
        if (Constant.JobState.TIMEOUT.equals(scriptHistory.getState())) {
            // 重试
            retryCurrentNode(scriptHistory, Constant.ErrorType.TIMEOUT);
        }
    }

    private void kill() {
        Matcher matcher = KILL_PATTERN.matcher(scriptHistory.getOutputs());
        if (!matcher.find()) {
            LOGGER.info("未能匹配时间标记，不能执行kill命令");
            return;
        }
        int date = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        DateFormat dateFormat;
        if (date < 10) {
            dateFormat = new SimpleDateFormat("EEE MMM  d HH:mm:ss yyyy", Locale.ENGLISH);
        } else {
            dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", Locale.ENGLISH);
        }
        long timestamp = Long.parseLong(matcher.group(1) + "000");
        String lstartM1 = dateFormat.format(new Date(timestamp - 1000));
        String lstart = dateFormat.format(new Date(timestamp));
        String lstartA1 = dateFormat.format(new Date(timestamp + 1000));
        String cmd;
        String commandTemplate;
        if (Constant.ScriptType.SHELL.equals(scriptHistory.getScriptType())) {
            cmd = scriptHistory.getContent().replaceAll("/\\*", "/\\\\*");
            commandTemplate = "kill -9 $(ps -eo pid,lstart,cmd | grep '%s %s' | grep -v 'grep' | grep -v 'echo time mark' | awk '{print $1}')";
        } else {
            String [] jobParams = scriptHistory.getJobParams().split(";");
            if (Constant.ScriptType.SPARK_BATCH.equals(scriptHistory.getScriptType()) || Constant.ScriptType.SPARK_STREAM.equals(scriptHistory.getScriptType())) {
                if (scriptHistory.getContent().indexOf("--queue " + jobParams[1]) > scriptHistory.getContent().indexOf("--name " + jobParams[2])) {
                    cmd = jobParams[2] + ".*" + jobParams[1];
                } else {
                    cmd = jobParams[1] + ".*" + jobParams[2];
                }
            } else {
                if (scriptHistory.getContent().indexOf("-yqu " + jobParams[1]) > scriptHistory.getContent().indexOf("-ynm " + jobParams[2])) {
                    cmd = jobParams[2] + ".*" + jobParams[1];
                } else {
                    cmd = jobParams[1] + ".*" + jobParams[2];
                }
            }
            commandTemplate = "kill -9 $(ps -eo pid,lstart,cmd | grep '%s.*%s' | grep -v 'grep' | grep -v 'echo time mark' | awk '{print $1}')";
        }
        String command = String.format(commandTemplate, lstartM1, cmd) + " ; " +
                String.format(commandTemplate, lstart, cmd) + " ; " +
                String.format(commandTemplate, lstartA1, cmd);
        Session session = null;
        try {
            session = conn.openSession();
            session.execCommand(command);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public static void build(ScriptHistory scriptHistory) {
        build(scriptHistory, null);
    }

    public static void build(ScriptHistory scriptHistory, Date startDate) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("timeout", scriptHistory.getTimeout());
        jobDataMap.put("submitTime", scriptHistory.getSubmitTime());
        SchedulerUtils.scheduleSimpleJob(ScriptJob.class, scriptHistory.getId(), Constant.JobGroup.SCRIPT_JOB, 0, 0, jobDataMap, startDate, null);
    }

    public static void destroy(Object name) {
        SchedulerUtils.interrupt(name, Constant.JobGroup.SCRIPT_JOB);
        SchedulerUtils.deleteJob(name, Constant.JobGroup.SCRIPT_JOB);
    }

}
