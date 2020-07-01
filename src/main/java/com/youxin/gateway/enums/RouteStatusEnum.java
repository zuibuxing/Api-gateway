package com.youxin.gateway.enums;

public enum RouteStatusEnum {


    /**
     * 路由状态枚举值
     */
    START("1", "启用"),
    STOP("0", "停止");


    RouteStatusEnum(String status, String description) {
        this.status = status;
        this.description = description;
    }

    private String status;

    public String getStatus() {
        return this.status;
    }

    private String description;

    public String getDescription() {
        return this.description;
    }
}
