package com.meiyou.bigwhale.scheduler.workflow;

import com.meiyou.bigwhale.common.Constant;
import com.meiyou.bigwhale.entity.ScriptHistory;
import com.meiyou.bigwhale.scheduler.job.ScriptJob;
import com.meiyou.bigwhale.service.ScriptHistoryService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author Suxy
 * @date 2020/4/23
 * @description file description
 */
@DisallowConcurrentExecution
public class ScheduleJobSubmitter implements Job {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private ScriptHistoryService scriptHistoryService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        List<ScriptHistory> scriptHistories = scriptHistoryService.findByQuery("scheduleId+;" +
                        "delayTime<=" + DATE_FORMAT.format(new Date()) + ";" +
                        "state=" + Constant.JobState.TIME_WAIT_,
                new Sort(Sort.Direction.ASC, "id"));
        for (ScriptHistory scriptHistory : scriptHistories) {
            scriptHistory.updateState(Constant.JobState.SUBMIT_WAIT);
            scriptHistory.updateState(Constant.JobState.SUBMITTING);
            scriptHistory = scriptHistoryService.save(scriptHistory);
            ScriptJob.build(scriptHistory);
        }
    }

}
