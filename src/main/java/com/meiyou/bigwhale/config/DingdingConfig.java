package com.meiyou.bigwhale.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Suxy
 * @date 2019/11/8
 * @description file description
 */
@Configuration
@ConfigurationProperties(prefix = "big-whale.dingding")
public class DingdingConfig {

    /**
     * 是否启用钉钉消息通知
     */
    private boolean enabled = false;
    /**
     * 公共频道监控
     */
    private String watcherToken;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getWatcherToken() {
        return watcherToken;
    }

    public void setWatcherToken(String watcherToken) {
        this.watcherToken = watcherToken;
    }

}
