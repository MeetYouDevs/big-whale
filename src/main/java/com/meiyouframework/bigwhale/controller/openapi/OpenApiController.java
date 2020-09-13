package com.meiyouframework.bigwhale.controller.openapi;

import com.alibaba.fastjson.JSON;
import com.meiyouframework.bigwhale.common.Constant;
import com.meiyouframework.bigwhale.common.pojo.Msg;
import com.meiyouframework.bigwhale.controller.BaseController;
import com.meiyouframework.bigwhale.entity.CmdRecord;
import com.meiyouframework.bigwhale.entity.Scheduling;
import com.meiyouframework.bigwhale.entity.Script;
import com.meiyouframework.bigwhale.entity.auth.User;
import com.meiyouframework.bigwhale.service.CmdRecordService;
import com.meiyouframework.bigwhale.service.SchedulingService;
import com.meiyouframework.bigwhale.service.ScriptService;
import com.meiyouframework.bigwhale.service.auth.UserService;
import com.meiyouframework.bigwhale.task.cmd.CmdRecordRunner;
import com.meiyouframework.bigwhale.task.timed.TimedTask;
import com.meiyouframework.bigwhale.util.SchedulerUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.quartz.JobDataMap;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * @author Suxy
 * @date 2020/4/15
 * @description file description
 */
@RestController
@RequestMapping("/openapi")
public class OpenApiController extends BaseController {

    private PasswordEncoder passwordEncoder = new StandardPasswordEncoder();

    @Autowired
    private ScriptService scriptService;
    @Autowired
    private UserService userService;
    @Autowired
    private SchedulingService schedulingService;
    @Autowired
    private CmdRecordService cmdRecordService;

    @RequestMapping(value = "/script/execute.api", method = RequestMethod.POST)
    public Msg scriptExecute(@RequestBody ExecuteArgs args) throws SchedulerException {
        if (StringUtils.isBlank(args.id) || StringUtils.isBlank(args.sign)) {
            return failed("参数错误");
        }
        Script script = scriptService.findById(args.id);
        if (script == null) {
            return failed("脚本不存在");
        }
        User user = userService.findById(script.getUid());
        if (user == null) {
            return failed("用户不存在");
        }
        String password = new String(Base64.decodeBase64(args.sign), StandardCharsets.UTF_8);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return failed("签名验证失败");
        }
        CmdRecord cmdRecord = CmdRecord.builder()
                .uid(script.getUid())
                .scriptId(script.getId())
                .status(Constant.EXEC_STATUS_UNSTART)
                .agentId(script.getAgentId())
                .clusterId(script.getClusterId())
                .content(script.getScript())
                .timeout(script.getTimeout())
                .createTime(new Date())
                .build();
        if (args.args != null && !args.args.isEmpty()) {
            cmdRecord.setArgs(JSON.toJSONString(args.args));
        }
        cmdRecord = cmdRecordService.save(cmdRecord);
        CmdRecordRunner.build(cmdRecord);
        return success(cmdRecord);
    }

    @RequestMapping(value = "/scheduling/execute.api", method = RequestMethod.POST)
    public Msg schedulingExecute(@RequestBody ExecuteArgs args) throws SchedulerException {
        if (StringUtils.isBlank(args.id) || StringUtils.isBlank(args.sign)) {
            return failed("参数错误");
        }
        Scheduling scheduling = schedulingService.findById(args.id);
        if (scheduling == null) {
            return failed("任务不存在");
        }
        User user = userService.findById(scheduling.getUid());
        if (user == null) {
            return failed("用户不存在");
        }
        if (scheduling.getType() == Constant.SCHEDULING_TYPE_STREAMING) {
            return failed("类型不支持");
        }
        String password = new String(Base64.decodeBase64(args.sign), StandardCharsets.UTF_8);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return failed("签名验证失败");
        }
        if (args.args != null && !args.args.isEmpty()) {
            JobDataMap jobDataMap = new JobDataMap(args.args);
            SchedulerUtils.scheduleSimpleJob(TimedTask.class, scheduling.getId(), Constant.JobGroup.TIMED_FOR_API, 0, 0, jobDataMap);
        } else {
            SchedulerUtils.scheduleSimpleJob(TimedTask.class, scheduling.getId(), Constant.JobGroup.TIMED_FOR_API, 0, 0);
        }
        return success();
    }

    static class ExecuteArgs {

        private String id;
        private String sign;
        private Map<String, String> args;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSign() {
            return sign;
        }

        public void setSign(String sign) {
            this.sign = sign;
        }

        public Map<String, String> getArgs() {
            return args;
        }

        public void setArgs(Map<String, String> args) {
            this.args = args;
        }
    }

}
