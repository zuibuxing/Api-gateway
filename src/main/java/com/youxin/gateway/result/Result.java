package com.youxin.gateway.result;


/**
 * @author sunxin02
 */
public class Result<T> {
    private int code;

    private String message;

    private T data;

    public Result() {
    }

    public Result(ResultCodeEnum resultCodeEnum) {
        this.code=resultCodeEnum.getCode();
        this.message=resultCodeEnum.getMessage();
    }

    public void setResultCodeEnum(ResultCodeEnum resultCodeEnum){
        this.code=resultCodeEnum.getCode();
        this.message=resultCodeEnum.getMessage();
    }

    public Result(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public Result(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
