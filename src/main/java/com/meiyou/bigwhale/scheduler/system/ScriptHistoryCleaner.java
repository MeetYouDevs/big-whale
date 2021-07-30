package com.meiyou.bigwhale.scheduler.system;

import com.meiyou.bigwhale.entity.ScriptHistory;
import com.meiyou.bigwhale.service.ScriptHistoryService;
import org.apache.commons.lang.time.DateUtils;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author Suxy
 * @date 2019/10/9
 * @description file description
 */
@DisallowConcurrentExecution
public class ScriptHistoryCleaner implements Job {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private ScriptHistoryService scriptHistoryService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        Date before = DateUtils.addMonths(new Date(), -3);
        String query = "createTime<" + DATE_FORMAT.format(before);
        List<ScriptHistory> scriptHistories = scriptHistoryService.findByQuery(query);
        scriptHistories.removeIf(ScriptHistory::isRunning);
        scriptHistoryService.deleteAll(scriptHistories);
    }
}
