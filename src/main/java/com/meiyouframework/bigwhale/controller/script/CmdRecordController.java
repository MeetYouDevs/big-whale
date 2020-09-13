package com.meiyouframework.bigwhale.controller.script;

import com.meiyouframework.bigwhale.common.pojo.Msg;
import com.meiyouframework.bigwhale.data.domain.PageRequest;
import com.meiyouframework.bigwhale.dto.DtoCmdRecord;
import com.meiyouframework.bigwhale.service.CmdRecordService;
import com.meiyouframework.bigwhale.controller.BaseController;
import com.meiyouframework.bigwhale.security.LoginUser;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/script/cmd_record")
public class CmdRecordController extends BaseController{

    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private CmdRecordService cmdRecordService;

    @RequestMapping(value = "/getpage.api", method = RequestMethod.POST)
    public Msg getPage(@RequestBody DtoCmdRecord req) {
        LoginUser user = getCurrentUser();
        if (!user.isRoot()) {
            req.setUid(user.getId());
        }
        List<String> tokens = new ArrayList<>();
        if (StringUtils.isNotBlank(req.getContent())) {
            tokens.add("content?" + req.getContent());
        }
        if (StringUtils.isNotBlank(req.getUid())) {
            tokens.add("uid=" + req.getUid());
        }
        if (StringUtils.isNotBlank(req.getAgentId())) {
            tokens.add("agentId=" + req.getAgentId());
        }
        if (StringUtils.isNotBlank(req.getClusterId())) {
            tokens.add("clusterId=" + req.getClusterId());
        }
        if (req.getStatus() != null) {
            tokens.add("status=" + req.getStatus());
        }
        if (req.getStart() != null) {
            tokens.add("createTime>=" + dateFormat.format(req.getStart()));
        }
        if (req.getEnd() != null) {
            tokens.add("createTime<=" + dateFormat.format(req.getEnd()));
        }
        if (req.getScriptId() != null) {
            tokens.add("scriptId=" + req.getScriptId());
        }
        if (req.getSchedulingId() != null) {
            tokens.add("schedulingId=" + req.getSchedulingId());
        }
        if (req.getId() != null) {
            tokens.add("id=" + req.getId());
        }
        Page<DtoCmdRecord> dtoCmdRecordPage = cmdRecordService.pageByQuery(new PageRequest(req.pageNo - 1, req.pageSize, StringUtils.join(tokens, ";"), new Sort(Sort.Direction.DESC, "createTime"))).map((item) -> {
            DtoCmdRecord dtoCmdRecord = new DtoCmdRecord();
            BeanUtils.copyProperties(item, dtoCmdRecord);
            return dtoCmdRecord;
        });
        return success(dtoCmdRecordPage);
    }
}
