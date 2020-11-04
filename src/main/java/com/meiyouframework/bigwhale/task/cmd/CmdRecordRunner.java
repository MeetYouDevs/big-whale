package com.meiyouframework.bigwhale.task.cmd;

import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.meiyouframework.bigwhale.common.Constant;
import com.meiyouframework.bigwhale.config.SshConfig;
import com.meiyouframework.bigwhale.entity.*;
import com.meiyouframework.bigwhale.service.*;
import com.meiyouframework.bigwhale.task.AbstractCmdRecordTask;
import com.meiyouframework.bigwhale.util.SchedulerUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Suxy
 * @date 2019/8/29
 * @description file description
 */
@DisallowConcurrentExecution
public class CmdRecordRunner extends AbstractCmdRecordTask implements InterruptableJob {

    private static final Pattern PATTERN = Pattern.compile("application_\\d+_\\d+");
    private static final Pattern PATTERN1 = Pattern.compile("time mark: (\\d+)");
    private static final Logger LOGGER = LoggerFactory.getLogger(CmdRecordRunner.class);

    private DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    private Thread thread;
    private volatile boolean commandFinish = false;
    private volatile boolean interrupted = false;
    private CmdRecord cmdRecord;
    private Script script;
    private String yarnUrl;
    private Connection conn;

    @Autowired
    private ScriptService scriptService;
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
        thread = Thread.currentThread();
        String cmdRecordId = jobExecutionContext.getJobDetail().getKey().getName();
        cmdRecord = cmdRecordService.findById(cmdRecordId);
        if (cmdRecord.getTimeout() == null) {
            cmdRecord.setTimeout(5);
        }
        Date timeoutTime = DateUtils.addMinutes(cmdRecord.getCreateTime(), cmdRecord.getTimeout());
        Date current = new Date();
        //超时的情况统一由CmdTimeoutJob处理
        if (current.after(timeoutTime)) {
            return;
        }
        //更新cmdRecord正在执行
        String now = dateFormat.format(current);
        try {
            cmdRecord.setStartTime(dateFormat.parse(now));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        cmdRecord.setStatus(Constant.EXEC_STATUS_DOING);
        cmdRecordService.save(cmdRecord);
        script = scriptService.findById(cmdRecord.getScriptId());
        Scheduling scheduling = StringUtils.isNotBlank(cmdRecord.getSchedulingId()) ? schedulingService.findById(cmdRecord.getSchedulingId()) : null;
        Session session = null;
        try {
            String instance;
            if (script.getType() != Constant.SCRIPT_TYPE_SHELL_BATCH) {
                yarnUrl = clusterService.findById(cmdRecord.getClusterId()).getYarnUrl();
                instance = agentService.getInstanceByClusterId(cmdRecord.getClusterId(), true);
                cmdRecord.setAgentInstance(instance);
            } else {
                instance = agentService.getInstanceByAgentId(cmdRecord.getAgentId(), true);
                cmdRecord.setAgentInstance(instance);
            }
            if (instance.contains(":")) {
                String [] arr = instance.split(":");
                conn = new Connection(arr[0], Integer.parseInt(arr[1]));
            } else {
                conn = new Connection(instance);
            }
            conn.connect(null, sshConfig.getConnectTimeout(), 30000);
            conn.authenticateWithPassword(sshConfig.getUser(), sshConfig.getPassword());
            int ret;
            session = conn.openSession();
            String command = cmdRecord.getContent();
            if (cmdRecord.getArgs() != null) {
                JSONObject argsObj = JSON.parseObject(cmdRecord.getArgs());
                for (Map.Entry<String, Object> entry : argsObj.entrySet()) {
                    command = command.replace(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
            //为确保调度流程的准确性，应用名称添加实例ID
            if (scheduling != null) {
                if (script.getType() == Constant.SCRIPT_TYPE_SPARK_BATCH) {
                    command = command.replace("--name " + script.getApp(), "--name " + script.getApp() + "_instance" + now);
                } else if (script.getType() == Constant.SCRIPT_TYPE_FLINK_BATCH) {
                    command = command.replace("-ynm " + script.getApp(), "-ynm " + script.getApp() + "_instance" + now);
                }
            }
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
                ret = session.getExitStatus();
                if (ret == 0) {
                    cmdRecord.setStatus(Constant.EXEC_STATUS_FINISH);
                    if (script.getType() == Constant.SCRIPT_TYPE_SHELL_BATCH) {
                        //Shell脚本提交子任务(定时任务)
                        submitNextNode(cmdRecord, scheduling, scriptService);
                    } else {
                        //设置yarn任务状态
                        cmdRecord.setJobFinalStatus("UNDEFINED");
                    }
                } else {
                    cmdRecord.setStatus(Constant.EXEC_STATUS_FAIL);
                    //处理失败(定时任务)
                    notice(cmdRecord, scheduling, null, Constant.ERROR_TYPE_FAILED);
                    //重试
                    retryCurrentNode(cmdRecord, scheduling);
                }
                cmdRecord.setFinishTime(new Date());
            } else {
                CmdRecord recordForTimeout = cmdRecordService.findById(cmdRecordId);
                cmdRecord.setStatus(recordForTimeout.getStatus());
            }
            cmdRecordService.save(cmdRecord);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            if (!interrupted) {
                cmdRecord.setStatus(Constant.EXEC_STATUS_FAIL);
                notice(cmdRecord, scheduling, null, Constant.ERROR_TYPE_FAILED);
                retryCurrentNode(cmdRecord, scheduling);
            } else {
                CmdRecord recordForTimeout = cmdRecordService.findById(cmdRecordId);
                cmdRecord.setStatus(recordForTimeout.getStatus());
            }
            cmdRecord.setErrors(e.getMessage());
            cmdRecordService.save(cmdRecord);
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
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) != -1) {
                if (out.size() < 61440) {
                    out.write(buffer, 0, length);
                    String tmp = out.toString("UTF-8");
                    if (stdout) {
                        cmdRecord.setOutputs(tmp);
                    } else {
                        cmdRecord.setErrors(tmp);
                    }
                    cmdRecordService.save(cmdRecord);
                }
            }
            if (out.size() > 0) {
                String content = out.toString("UTF-8");
                if (stdout) {
                    cmdRecord.setOutputs(content);
                } else {
                    cmdRecord.setErrors(content);
                }
                if (yarnUrl != null) {
                    this.extraInfo(yarnUrl, content);
                }
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 提取yarn信息
     * @param yarnUrl
     * @param message
     * @return
     */
    private void extraInfo(String yarnUrl, String message) {
        if (cmdRecord.getJobId() == null) {
            Matcher matcher = PATTERN.matcher(message);
            if (matcher.find()) {
                String id = matcher.group();
                cmdRecord.setJobId(id);
                cmdRecord.setJobUrl(yarnUrl + "/proxy/" + id + "/");
            }
        }
    }

    private void kill() {
        Matcher matcher = PATTERN1.matcher(cmdRecord.getOutputs());
        if (!matcher.find()) {
            LOGGER.error("未能匹配时间标记，不能执行kill命令");
        }
        DateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", Locale.ENGLISH);
        long timestamp = Long.parseLong(matcher.group(1) + "000");
        String lstartM1 = dateFormat.format(new Date(timestamp - 1000));
        String lstart = dateFormat.format(new Date(timestamp));
        String lstartA1 = dateFormat.format(new Date(timestamp + 1000));
        String cmd;
        String commandTemplate;
        if (script.getType() == Constant.SCRIPT_TYPE_SHELL_BATCH) {
            cmd = cmdRecord.getContent().replaceAll("/\\*", "/\\\\*");
            commandTemplate = "kill -9 $(ps -eo pid,lstart,cmd | grep '%s %s' | grep -v 'grep' | grep -v 'echo time mark' | awk '{print $1}')";
        } else {
            if (script.getType() == Constant.SCRIPT_TYPE_SPARK_STREAMING || script.getType() == Constant.SCRIPT_TYPE_SPARK_BATCH) {
                if (script.getScript().indexOf("--queue " + script.getQueue()) > script.getScript().indexOf("--name " + script.getApp())) {
                    cmd = script.getApp() + ".*" + script.getQueue();
                } else {
                    cmd = script.getQueue() + ".*" + script.getApp();
                }
            } else {
                if (script.getScript().indexOf("-yqu " + script.getQueue()) > script.getScript().indexOf("-ynm " + script.getApp())) {
                    cmd = script.getApp() + ".*" + script.getQueue();
                } else {
                    cmd = script.getQueue() + ".*" + script.getApp();
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

    public static void build(CmdRecord cmdRecord) throws SchedulerException {
        SchedulerUtils.scheduleSimpleJob(CmdRecordRunner.class, cmdRecord.getId(), Constant.JobGroup.CMD, 0, 0);
    }

    public static void build(CmdRecord cmdRecord, Date startDate) throws SchedulerException {
        SchedulerUtils.scheduleSimpleJob(CmdRecordRunner.class, cmdRecord.getId(), Constant.JobGroup.CMD, 0, 0, null, startDate, null);
    }

}
