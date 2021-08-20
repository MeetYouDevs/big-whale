package com.meiyou.bigwhale.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.meiyou.bigwhale.common.Constant;
import com.meiyou.bigwhale.config.DingdingConfig;
import com.meiyou.bigwhale.util.OkHttpUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class NoticeServiceImpl implements NoticeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoticeServiceImpl.class);
    private static final Pattern MAIL_PATTERN = Pattern.compile("^\\w+((-\\w+)|(\\.\\w+))*\\@[A-Za-z0-9]+((\\.|-)[A-Za-z0-9]+)*\\.[A-Za-z0-9]+$");

    @Value("${spring.mail.username}")
    private String from;

    @Autowired
    private JavaMailSender javaMailSender;
    @Autowired
    private DingdingConfig dingdingConfig;

    @Override
    public void sendEmail(String to, String content) {
        if (!MAIL_PATTERN.matcher(to).find()) {
            LOGGER.error("illegal email address, use console\n" + content);
            return;
        }
        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setFrom(from);
        simpleMailMessage.setTo(to);
        simpleMailMessage.setSubject("巨鲸任务调度平台");
        simpleMailMessage.setText(content);
        try {
            javaMailSender.send(simpleMailMessage);
        } catch (Exception e) {
            LOGGER.error("send email error", e);
        }
    }

    @Override
    public void sendDingding(String[] ats, String content) {
        if (!dingdingConfig.isEnabled()) {
            LOGGER.error("dingding alarm is not enabled, use console\n" + content);
            return;
        }
        if (StringUtils.isBlank(dingdingConfig.getWatcherToken())) {
            LOGGER.error("dingding public watch token is not set, use console\n" + content);
            return;
        }
        sendDingding(dingdingConfig.getWatcherToken(), ats, content);
    }

    @Override
    public void sendDingding(String token, String[] ats, String content) {
        if (!dingdingConfig.isEnabled()) {
            LOGGER.error("dingding alarm is not enabled, use console\n" + content);
            return;
        }
        Map<String, Object> reqBody = new HashMap<>();
        reqBody.put("msgtype", "text");
        reqBody.put("text", Collections.singletonMap("content", content));
        if (ats != null && ats.length > 0) {
            Map<String, Object> atMap = new HashMap<>();
            atMap.put("isAtAll", "false");
            atMap.put("atMobiles", ats);
            reqBody.put("at", atMap);
        }
        OkHttpUtils.Result result = OkHttpUtils.doPost(Constant.DINGDING_ROBOT_URL + token, OkHttpUtils.MEDIA_JSON, JSON.toJSONString(reqBody), null);
        if (result.isSuccessful) {
            JSONObject obj = JSON.parseObject(result.content);
            if (obj.getIntValue("errcode") != 0) {
                LOGGER.error("dingding alarm failed with error message: [" + obj.getString("errmsg") + "], use console\n" + content);
            }
        }
    }

    @Override
    public boolean isWatcherToken(String token) {
        return dingdingConfig.isEnabled() &&
                dingdingConfig.getWatcherToken() != null &&
                dingdingConfig.getWatcherToken().equals(token.split("&")[0]);
    }
}
