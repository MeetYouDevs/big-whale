package com.meiyouframework.bigwhale.task.common;

import com.meiyouframework.bigwhale.service.CmdRecordService;
import org.apache.commons.lang.time.DateUtils;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Suxy
 * @date 2019/10/9
 * @description file description
 */
@DisallowConcurrentExecution
public class CmdRecordClearJob implements Job {

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private CmdRecordService cmdRecordService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        Date before = DateUtils.addMonths(new Date(), -3);
        String query = "createTime<" + DATE_FORMAT.format(before);
        cmdRecordService.deleteByQuery(query);
    }
}
