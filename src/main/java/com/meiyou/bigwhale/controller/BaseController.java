package com.meiyou.bigwhale.controller;


import com.meiyou.bigwhale.common.pojo.Msg;
import com.meiyou.bigwhale.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;

/**
 * @author progr1mmer
 */
public abstract class BaseController {

    @Autowired
    private HttpServletRequest request;

    protected LoginUser getCurrentUser() {
        return (LoginUser) request.getSession().getAttribute("user");
    }

    protected Msg success() {
        return success(null);
    }

    protected Msg success(Object content) {
        return success("操作成功", content);
    }

    protected Msg success(String msg, Object content) {
        return Msg.create(0, msg, content);
    }

    protected Msg failed() {
        return failed("操作失败");
    }

    protected Msg failed(String msg) {
        return failed(-1, msg);
    }

    protected Msg failed(int code, String msg) {
        return Msg.create(code, msg, null);
    }

}
