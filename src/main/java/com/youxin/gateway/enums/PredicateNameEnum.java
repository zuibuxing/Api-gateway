package com.youxin.gateway.enums;


import com.sun.el.util.ReflectionUtil;
import com.youxin.gateway.model.GatewayPredicateDefinition;

import java.util.Arrays;

/**
 * 断言器名字枚举
 */

public enum PredicateNameEnum {
    /**
     * 断言器名字枚举,这些是gate自定义了的断言器类型的名字
     *  保存自定义路由的时候 RouteDefinitionRouteLocato会按照name去寻找所有实现了 RoutePredicateFactory 的类,name是类名去掉RoutePredicateFactory
     * 如果是随便写的，会导致加载bean找不到断言器报错
     */
    AFTER("After"),
    BETWEEN("Between"),
    CLOUDFOUNDRYROUTESERVICE("CloudFoundryRouteService"),
    COOKIE("Cookie"),
    HEADER("Header"),
    HOST("Host"),
    METHOD("Method"),
    PATH("Path"),
    QUERY("Query"),
    READBODYPREDICATEFACTORY("ReadBodyPredicateFactory"),
    REMOTEADDR("RemoteAddr"),
    WEIGHT("Weight");

    PredicateNameEnum(String name) {
        this.name = name;
    }

    private String name;

    public String getName() {
        return this.name;
    }


    /**
     * 校验断言器的名称是否合法
     *
     * @param name
     * @return
     */
    public static boolean checkPredicateName(String name) {
        boolean flag = false;
        for (PredicateNameEnum predicateNameEnum : PredicateNameEnum.values()) {
            if (predicateNameEnum.getName().equals(name)) {
                flag = true;
                break;
            }
        }
        return flag;
    }

    public static void main(String[] args) {
        for (PredicateNameEnum predicateNameEnum : PredicateNameEnum.values()) {
            System.out.println(predicateNameEnum.getName().toUpperCase());
        }
    }

}
