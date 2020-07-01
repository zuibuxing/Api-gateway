package com.youxin.gateway.enums;


/**
 * 路由操作结果枚举值
 */
public enum GatewayResultEnum {
    /**
     * 路由操作结果枚举值
     */
    PREDICATE_EMPTY("断言信息不能为空!"),
    PREDICATE_NAME_ERROR("断言器名称不合法!"),
    FILTER_NAME_ERROR("过滤器名称不合法!"),
    FILTER_ARGS_EMPTY("过滤器缺少必要参数!"),
    KEYRESOLVER_NAME_ERROR("key-solver名称不合法!"),
    KEYRESOLVER_LIMIT_ARGS_ERROR("key-solver限流参数不合法!"),
    ADD_ROUTE_SUCCESS("添加路由成功!"),
    ADD_ROUTE_FALSE("添加路由失败!"),
    UPDATE_ROUTE_SUCCESS("更新路由成功"),
    UPDATE_ROUTE_FALSE("更新路由失败!"),
    DELETE_ROUTE_SUCCESS("删除路由成功"),
    DELETE_ROUTE_FALSE("删除路由失败!"),
    REFRESH_ROUTE_SUCCESS("刷新路由成功!"),
    REFRESH_ROUTE_FALSE("刷新路由失败!"),
    NOTIFY_SERVICE_SUCCESS("通知服务更新成功!"),
    NOTIFY_SERVICE_FAIL("通知服务更新失败!");



    GatewayResultEnum(String name) {
        this.name = name;
    }

    private String name;

    public String getName() {
        return this.name;
    }


}
