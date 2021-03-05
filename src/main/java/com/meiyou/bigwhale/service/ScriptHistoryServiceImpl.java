package com.meiyou.bigwhale.service;

import com.meiyou.bigwhale.common.Constant;
import com.meiyou.bigwhale.data.service.AbstractMysqlPagingAndSortingQueryService;
import com.meiyou.bigwhale.entity.ScriptHistory;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class ScriptHistoryServiceImpl extends AbstractMysqlPagingAndSortingQueryService<ScriptHistory, Integer> implements ScriptHistoryService {

    @Override
    public ScriptHistory findNoScheduleLatestByScriptId(Integer scriptId) {
        return findOneByQuery("scriptId=" + scriptId + ";scheduleId-", new Sort(Sort.Direction.DESC, "createTime"));
    }

    @Override
    public void missingScheduling(ScriptHistory scriptHistory) {
        scriptHistory.updateState(Constant.JobState.FAILED);
        // scriptHistory.setFinishTime(new Date());
        scriptHistory.setErrors("Missing scheduling");
        save(scriptHistory);
    }

    @Override
    public boolean execTimeout(ScriptHistory scriptHistory) {
        Date ago = DateUtils.addMinutes(new Date(), -scriptHistory.getTimeout());
        // 执行超时
        Date time;
        if (scriptHistory.getStartTime() != null) {
            time = scriptHistory.getStartTime();
        } else if (scriptHistory.getScheduleHistoryTime() != null) {
            time = scriptHistory.getScheduleHistoryTime();
        } else {
            time = scriptHistory.getCreateTime();
        }
        return time.compareTo(ago) <= 0;
    }

}
