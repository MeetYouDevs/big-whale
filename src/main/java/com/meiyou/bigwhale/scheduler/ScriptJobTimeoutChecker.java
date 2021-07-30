package com.meiyou.bigwhale.scheduler;

import com.meiyou.bigwhale.common.Constant;
import com.meiyou.bigwhale.scheduler.job.ScriptJob;
import com.meiyou.bigwhale.util.SchedulerUtils;
import org.quartz.*;

import java.util.*;

/**
 * @author Suxy
 * @date 2019/9/6
 * @description file description
 */
@DisallowConcurrentExecution
public class ScriptJobTimeoutChecker extends AbstractRetryable implements Job {

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        List<JobExecutionContext> executionContexts;
        try {
            executionContexts = SchedulerUtils.getScheduler().getCurrentlyExecutingJobs();
        } catch (SchedulerException e) {
            e.printStackTrace();
            return;
        }
        for (JobExecutionContext executionContext : executionContexts) {
            JobKey jobKey = executionContext.getJobDetail().getKey();
            if (Constant.JobGroup.SCRIPT_JOB.equals(jobKey.getGroup())) {
                JobDataMap jobDataMap = executionContext.getMergedJobDataMap();
                Integer timeout = (Integer) jobDataMap.get("timeout");
                Date submitTime = (Date) jobDataMap.get("submitTime");
                if ((System.currentTimeMillis() - submitTime.getTime()) > (timeout * 60 * 1000)) {
                    jobDataMap.put("timeout", true);
                    ScriptJob.destroy(jobKey.getName());
                }
            }
        }
    }

}
