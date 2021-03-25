package com.meiyou.bigwhale.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

/**
 * @author progr1mmer
 * @date Created on 2020/3/18
 */
@Configuration
@ConfigurationProperties(prefix = "big-whale.ssh")
public class SshConfig {

    /**
     * ssh默认用户
     */
    private String user;
    /**
     * ssh默认密码
     */
    private String password;
    /**
     * 连接超时
     */
    private int connectTimeout = 5000;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        Assert.notNull(user, "ssh user must be present");
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        Assert.notNull(password, "ssh password must be present");
        this.password = password;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }
}
