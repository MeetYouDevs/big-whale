package com.meiyou.bigwhale.common.pojo;

import java.io.Serializable;

/**
 * @author Suxy
 * @date 2019/10/28
 * @description file description
 */
public class Msg implements Serializable {

    private int code = 0;
    private String msg;
    private Object content;

    public Msg() {
    }

    public Msg(int code, String msg, Object content) {
        this.code = code;
        this.msg = msg;
        this.content = content;
    }

    public static Msg create(int code, String msg, Object content) {
        return new Msg(code, msg, content);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return this.msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Object getContent() {
        return this.content;
    }

    public void setContent(Object content) {
        this.content = content;
    }

}
