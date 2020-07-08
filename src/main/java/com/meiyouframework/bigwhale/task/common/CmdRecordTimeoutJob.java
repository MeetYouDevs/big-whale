package com.meiyouframework.bigwhale.task.common;

import com.meiyouframework.bigwhale.common.Constant;
import com.meiyouframework.bigwhale.entity.Scheduling;
import com.meiyouframework.bigwhale.service.SchedulingService;
import com.meiyouframework.bigwhale.task.AbstractCmdRecordTask;
import com.meiyouframework.bigwhale.util.SchedulerUtils;
import com.meiyouframework.bigwhale.util.SpringContextUtils;
import com.meiyouframework.bigwhale.entity.CmdRecord;
import com.meiyouframework.bigwhale.service.CmdRecordService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @author Suxy
 * @date 2019/9/6
 * @description file description
 */
@DisallowConcurrentExecution
public class CmdRecordTimeoutJob extends AbstractCmdRecordTask implements Job {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmdRecordTimeoutJob.class);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        //未开始执行和正在执行的
        CmdRecordService cmdRecordService = SpringContextUtils.getBean(CmdRecordService.class);
        SchedulingService schedulingService = SpringContextUtils.getBean(SchedulingService.class);
        Collection<CmdRecord> cmdRecords = cmdRecordService.findByQuery("status=" + Constant.EXEC_STATUS_UNSTART + "," + Constant.EXEC_STATUS_DOING);
        if (CollectionUtils.isEmpty(cmdRecords)) {
            return;
        }
        for (CmdRecord cmdRecord : cmdRecords) {
            Date now = new Date();
            if (cmdRecord.getTimeOut() == null) {
                cmdRecord.setTimeOut(5);
            }
            int timeOut = cmdRecord.getTimeOut();
            long ago = DateUtils.addMinutes(now, -timeOut).getTime();
            boolean timeout = false;
            //执行超时
            if (cmdRecord.getStatus() == Constant.EXEC_STATUS_DOING
                    && cmdRecord.getStartTime().getTime() <= ago) {
                cmdRecord.setStatus(Constant.EXEC_STATUS_TIMEOUT);
                timeout = true;
            } else {
                if (cmdRecord.getCreateTime().getTime() <= ago) {
                    cmdRecord.setStatus(Constant.EXEC_STATUS_TIMEOUT);
                    timeout = true;
                }
            }
            if (timeout) {
                Scheduling scheduling = StringUtils.isNotBlank(cmdRecord.getSchedulingId()) ? schedulingService.findById(cmdRecord.getSchedulingId()) : null;
                notice(cmdRecord, null, scheduling, null, Constant.ERROR_TYPE_TIMEOUT);
                cmdRecordService.save(cmdRecord);
                //处理调度
                try {
                    JobKey jobKey = new JobKey(cmdRecord.getId(), Constant.JobGroup.CMD);
                    if (cmdRecord.getStatus() == Constant.EXEC_STATUS_UNSTART) {
                        SchedulerUtils.getScheduler().deleteJob(jobKey);
                    } else {
                        SchedulerUtils.getScheduler().interrupt(jobKey);
                    }
                } catch (SchedulerException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }

}
