package com.meiyouframework.bigwhale.task.cmd;

import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.meiyouframework.bigwhale.common.Constant;
import com.meiyouframework.bigwhale.config.SshConfig;
import com.meiyouframework.bigwhale.entity.Scheduling;
import com.meiyouframework.bigwhale.service.*;
import com.meiyouframework.bigwhale.task.AbstractCmdRecordTask;
import com.meiyouframework.bigwhale.util.SchedulerUtils;
import com.meiyouframework.bigwhale.util.SpringContextUtils;
import com.meiyouframework.bigwhale.entity.Agent;
import com.meiyouframework.bigwhale.entity.CmdRecord;
import com.meiyouframework.bigwhale.entity.Script;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.quartz.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
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

    private Thread thread;
    private volatile boolean commandFinish = false;
    private volatile boolean interrupted = false;
    private CmdRecordService cmdRecordService;
    private CmdRecord cmdRecord;
    private String yarnUrl;

    @Override
    public void interrupt() {
        if (!commandFinish && !interrupted) {
            interrupted = true;
            thread.interrupt();
        }
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        thread = Thread.currentThread();
        String cmdRecordId = jobExecutionContext.getJobDetail().getKey().getName();
        cmdRecordService = SpringContextUtils.getBean(CmdRecordService.class);
        cmdRecord = cmdRecordService.findById(cmdRecordId);
        if (cmdRecord.getTimeOut() == null) {
            cmdRecord.setTimeOut(5);
        }
        Date timeoutTime = DateUtils.addMinutes(cmdRecord.getCreateTime(), cmdRecord.getTimeOut());
        //超时的情况统一由CmdTimeoutJob处理
        if (new Date().after(timeoutTime)) {
            return;
        }
        //更新cmdRecord正在执行
        cmdRecord.setStatus(Constant.EXEC_STATUS_DOING);
        cmdRecord.setStartTime(new Date());
        cmdRecordService.save(cmdRecord);
        AgentService agentService = SpringContextUtils.getBean(AgentService.class);
        Agent agent = agentService.findById(cmdRecord.getAgentId());
        ScriptService scriptService = SpringContextUtils.getBean(ScriptService.class);
        Script script = scriptService.findById(cmdRecord.getScriptId());
        if (script.getType() != Constant.SCRIPT_TYPE_SHELL) {
            ClusterService clusterService = SpringContextUtils.getBean(ClusterService.class);
            yarnUrl = clusterService.findById(cmdRecord.getClusterId()).getYarnUrl();
        }
        SchedulingService schedulingService = SpringContextUtils.getBean(SchedulingService.class);
        Connection conn = new Connection(agent.getIp());
        Session session = null;
        try {
            SshConfig sshConfig = SpringContextUtils.getBean(SshConfig.class);
            conn.connect(null, sshConfig.getConnectTimeout(), 30000);
            int ret;
            boolean isAuthenticated = conn.authenticateWithPassword(sshConfig.getUser(), sshConfig.getPassword());
            if (!isAuthenticated) {
                throw new IllegalArgumentException("Incorrect username or password");
            }
            agent.setLastConnTime(new Date());
            agentService.save(agent);
            session = conn.openSession();
            String command = cmdRecord.getContent();
            if (cmdRecord.getArgs() != null) {
                JSONObject argsObj = JSON.parseObject(cmdRecord.getArgs());
                for (Map.Entry<String, Object> entry : argsObj.entrySet()) {
                    command = command.replace(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
            session.execCommand(command);
            //异步执行
            if (!interrupted) {
                ExecutorService executorService = Executors.newFixedThreadPool(2);
                try {
                    CountDownLatch countDownLatch = new CountDownLatch(2);
                    final InputStream stdOut = new StreamGobbler(session.getStdout());
                    final InputStream stdErr = new StreamGobbler(session.getStderr());
                    executorService.execute(() -> {
                        try {
                            readOutput(true, stdOut);
                        } finally {
                            countDownLatch.countDown();
                        }
                    });
                    executorService.execute(() -> {
                        try {
                            readOutput(false, stdErr);
                        } finally {
                            countDownLatch.countDown();
                        }
                    });
                    countDownLatch.await();
                } catch (InterruptedException e) {

                } finally {
                    executorService.shutdown();
                }
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
                    if (script.getType() == Constant.SCRIPT_TYPE_SHELL) {
                        //Shell脚本提交子任务(定时任务)
                        submitSubCmdRecord(cmdRecord, cmdRecordService, agentService, scriptService);
                    } else {
                        //设置yarn任务状态
                        if (script.getType() == Constant.SCRIPT_TYPE_SPARK_BATCH || script.getType() == Constant.SCRIPT_TYPE_FLINK_BATCH) {
                            cmdRecord.setJobFinalStatus("UNDEFINED");
                        }
                    }
                } else {
                    cmdRecord.setStatus(Constant.EXEC_STATUS_FAIL);
                    //处理失败(定时任务)
                    Scheduling scheduling = StringUtils.isNotBlank(cmdRecord.getSchedulingId()) ? schedulingService.findById(cmdRecord.getSchedulingId()) : null;
                    notice(cmdRecord, null, scheduling, null, Constant.ERROR_TYPE_FAILED);
                }
                cmdRecord.setFinishTime(new Date());
            } else {
                CmdRecord recordForTimeout = cmdRecordService.findById(cmdRecordId);
                cmdRecord.setStatus(recordForTimeout.getStatus());
            }
            cmdRecordService.save(cmdRecord);
        } catch (Exception e) {
            if (!interrupted) {
                cmdRecord.setStatus(Constant.EXEC_STATUS_FAIL);
                Scheduling scheduling = StringUtils.isNotBlank(cmdRecord.getSchedulingId()) ? schedulingService.findById(cmdRecord.getSchedulingId()) : null;
                notice(cmdRecord, null, scheduling, null, Constant.ERROR_TYPE_FAILED);
            } else {
                CmdRecord recordForTimeout = cmdRecordService.findById(cmdRecordId);
                cmdRecord.setStatus(recordForTimeout.getStatus());
            }
            cmdRecord.setErrors(e.getMessage());
            cmdRecordService.save(cmdRecord);
            LOGGER.error(e.getMessage(), e);
        } finally {
            if (session != null) {
                session.close();
            }
            conn.close();
        }
    }

    private void readOutput(boolean stdout, InputStream in) {
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
                cmdRecord.setUrl(yarnUrl + "/proxy/" + id + "/");
            }
        }
    }

    public static void build(CmdRecord cmdRecord) throws SchedulerException {
        SchedulerUtils.scheduleSimpleJob(CmdRecordRunner.class, cmdRecord.getId(), Constant.JobGroup.CMD, 0, 0);
    }

}
