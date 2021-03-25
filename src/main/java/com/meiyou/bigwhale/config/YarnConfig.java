package com.meiyou.bigwhale.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Suxy
 * @date 2019/11/8
 * @description file description
 */
@Configuration
@ConfigurationProperties(prefix = "big-whale.yarn")
public class YarnConfig {

    /**
     * 普通应用可申请的内存上限，单位: MB
     * 为0禁用检查
     */
    private int appMemoryThreshold = 0;
    /**
     * 白名单应用
     * 可申请的内存无限制
     */
    private List<String> appWhiteList = new ArrayList<>();

    public int getAppMemoryThreshold() {
        return appMemoryThreshold;
    }

    public void setAppMemoryThreshold(int appMemoryThreshold) {
        this.appMemoryThreshold = appMemoryThreshold;
    }

    public List<String> getAppWhiteList() {
        return appWhiteList;
    }

    public void setAppWhiteList(List<String> appWhiteList) {
        this.appWhiteList = appWhiteList;
    }

}
