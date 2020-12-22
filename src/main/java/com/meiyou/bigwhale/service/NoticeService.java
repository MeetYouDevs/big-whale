package com.meiyou.bigwhale.service;

public interface NoticeService {

    void sendEmail(String to, String content);

    /**
     * 发送公共群（不设置公共群Token则不发送）
     * @param ats
     * @param content
     * @return
     */
    void sendDingding(String[] ats, String content);

    /**
     * 发送指定群
     * @param token
     * @param ats
     * @param content
     * @return
     */
    void sendDingding(String token, String[] ats, String content);

    /**
     * 是否公共群
     * @param token
     * @return
     */
    boolean isWatcherToken(String token);
}
