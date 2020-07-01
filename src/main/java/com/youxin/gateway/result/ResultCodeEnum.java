package com.youxin.gateway.result;

public enum ResultCodeEnum {

    //成功
    SUCCESS(200, "操作成功"),
    //失败
    ERROR(999, "操作失败");
    int code;
    String message;


    ResultCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
