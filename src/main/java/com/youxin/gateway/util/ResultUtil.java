package com.youxin.gateway.util;

import com.youxin.gateway.enums.GatewayResultEnum;
import com.youxin.gateway.result.Result;
import com.youxin.gateway.result.ResultCodeEnum;


/**
 * 返回结果工具类
 */
public class ResultUtil {

    public static Result<String> success(GatewayResultEnum gatewayResultEnum) {
        Result<String> result = new Result(ResultCodeEnum.SUCCESS);
        result.setData(gatewayResultEnum.getName());
        return result;
    }

    public static <T> Result<T> success(GatewayResultEnum gatewayResultEnum, T data) {
        Result<T> result = new Result(ResultCodeEnum.SUCCESS);
        result.setData(data);
        return result;
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result(ResultCodeEnum.SUCCESS);
        result.setData(data);
        return result;
    }

    public static Result<String> fail(GatewayResultEnum gatewayResultEnum) {
        Result<String> result = new Result(ResultCodeEnum.ERROR.getCode(), gatewayResultEnum.getName());
        return result;
    }

    public static Result<String> fail(String message) {
        Result<String> result = new Result(ResultCodeEnum.ERROR.getCode(), message);
        return result;
    }

    public static <T> Result<T> fail(String message, T data) {
        Result<T> result = new Result(ResultCodeEnum.ERROR.getCode(), message);
        result.setData(data);
        return result;
    }

    public static <T> Result<T> fail() {
        Result result = new Result(ResultCodeEnum.ERROR);
        result.setData(null);
        return result;
    }



    /**
     * 判断拉取结果是否合法
     *
     * @param result 输入map
     * @return flag
     */
    public static  boolean resultIsAvailabe(Result result) {
        return null != result && ResultCodeEnum.SUCCESS.getCode() == result.getCode();
    }

}
