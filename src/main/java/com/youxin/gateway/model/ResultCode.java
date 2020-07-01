package com.youxin.gateway.model;


import lombok.Data;

/**
 *
 * @author huangting
 */
public enum ResultCode {

    SUCCESS("20000", "success"),
    FAIL("50000", "fail");

    ResultCode(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    private String code;
    private String msg;


    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
