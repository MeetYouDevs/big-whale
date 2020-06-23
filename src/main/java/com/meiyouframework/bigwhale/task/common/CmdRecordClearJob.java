package com.meiyouframework.bigwhale.task.common;

import com.meiyouframework.bigwhale.service.CmdRecordService;
import com.meiyouframework.bigwhale.util.SpringContextUtils;
import org.apache.commons.lang.time.DateUtils;
import org.quartz.*;
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

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        CmdRecordService cmdRecordService = SpringContextUtils.getBean(CmdRecordService.class);
        Date before = DateUtils.addMonths(new Date(), -3);
        String query = "createTime<" + DATE_FORMAT.format(before);
        cmdRecordService.deleteByQuery(query);
    }
}
