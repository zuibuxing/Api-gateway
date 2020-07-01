package com.youxin.gateway.enums;


import com.youxin.gateway.exception.RouteErrorException;
import com.youxin.gateway.model.GatewayFilterDefinition;
import com.youxin.gateway.util.CollectionUtil;
import com.youxin.gateway.util.SpringContextUtil;
import org.springframework.context.ApplicationContext;

import java.util.Iterator;
import java.util.Map;

/**
 * 断言器名字枚举
 */

public enum FilterNameEnum {
    /**
     * filter名字枚举,这些是gate自定义了的断言器类型的名字
     * 保存自定义路由的时候 RouteDefinitionRouteLocato会按照name去寻找所有实现了 GatewayFilterFactory 的类,name是类名去掉GatewayFilterFactory
     * 如果是随便写的，会导致加载bean找不到filter报错
     */
    SETPATH("SetPath"),
    REQUESTHEADERTOREQUESTURI("RequestHeaderToRequestUri"),
    REQUESTHEADERSIZE("RequestHeaderSize"),
    REMOVEREQUESTHEADER("RemoveRequestHeader"),
    HYSTRIX("Hystrix"),
    MODIFYREQUESTBODY("ModifyRequestBody"),
    ADDREQUESTPARAMETER("AddRequestParameter"),
    REWRITELOCATIONRESPONSEHEADER("RewriteLocationResponseHeader"),
    MAPREQUESTHEADER("MapRequestHeader"),
    DEDUPERESPONSEHEADER("DedupeResponseHeader"),
    REQUESTRATELIMITER("RequestRateLimiter"),
    PRESERVEHOSTHEADER("PreserveHostHeader"),
    REWRITEPATH("RewritePath"),
    SETSTATUS("SetStatus"),
    SETREQUESTHEADER("SetRequestHeader"),
    PREFIXPATH("PrefixPath"),
    SAVESESSION("SaveSession"),
    STRIPPREFIX("StripPrefix"),
    MODIFYRESPONSEBODY("ModifyResponseBody"),
    REQUESTSIZE("RequestSize"),
    REDIRECTTO("RedirectTo"),
    SETRESPONSEHEADER("SetResponseHeader"),
    SECUREHEADERS("SecureHeaders"),
    ADDRESPONSEHEADER("AddResponseHeader"),
    FALLBACKHEADERS("FallbackHeaders"),
    RETRY("Retry"),
    ADDREQUESTHEADER("AddRequestHeader"),
    REMOVERESPONSEHEADER("RemoveResponseHeader"),
    REWRITERESPONSEHEADER("RewriteResponseHeader");

    /**
     * 限流时使用的 keySolver
     */
    private static String resolverKey = "key-resolver";
    /**
     * 限流时 token 每秒生成速率
     */
    private static String replenishRateKey = "redis-rate-limiter.replenishRate";
    /**
     * 限流时 token 总容量
     */
    private static String burstCapacityKey = "redis-rate-limiter.burstCapacity";


    /**
     * 重写 url 时原始url
     */
    private static String regexpKey = "regexp";
    /**
     * 重写后的 url
     */
    private static String replacementKey = "replacement";


    FilterNameEnum(String name) {
        this.name = name;
    }

    private String name;

    public String getName() {
        return this.name;
    }

    /**
     * 校验过滤器的名称是否合法
     *
     * @param name
     * @return
     */
    public static boolean checkFilterName(String name) {
        boolean flag = false;
        for (FilterNameEnum filterNameEnum : FilterNameEnum.values()) {
            if (filterNameEnum.getName().equals(name)) {
                flag = true;
                break;
            }
        }
        return flag;
    }


    /**
     * 校验限流filter的参数是否合法
     *
     * @param flter
     * @return
     */
    public static void checkRequestLimitArgs(GatewayFilterDefinition flter) throws Exception {
        Map<String, String> args = flter.getArgs();
        if (CollectionUtil.isEmpty(args)) {
            throw new RouteErrorException(GatewayResultEnum.FILTER_NAME_ERROR.getName());
        }
        //判断是否包含了必要的3个参数，resolverBean，token容积，token生产速率
        if (!args.containsKey(resolverKey) || !args.containsKey(replenishRateKey) || !args.containsKey(burstCapacityKey)) {
            throw new RouteErrorException(GatewayResultEnum.FILTER_ARGS_EMPTY.getName());
        }

        Iterator<Map.Entry<String, String>> iterator = args.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            if (resolverKey.equals(entry.getKey())) {
                //校验 ResolverBean是否合法
                checkResolverBean(entry);
            }
            // 是否配置了 token的容积和产生率，若没配置或小于1，抛出异常
            else if (replenishRateKey.equals(entry.getKey()) || burstCapacityKey.equals(entry.getKey())) {
                checkTokenArgs(entry);
            }
            //把这三个以外的参数移除，防止随便乱填
            else {
                iterator.remove();
            }
        }
    }

    /**
     * 校验 ResolverBean是否合法
     *
     * @param entry
     * @throws Exception
     */
    private static void checkResolverBean(Map.Entry<String, String> entry) throws Exception {
        //key-resolver 采用 spel表达式方式填写，例如  #{@ipKeyResolver}
        if (!entry.getValue().startsWith("#{@") || !entry.getValue().endsWith("}")) {
            throw new RouteErrorException(GatewayResultEnum.KEYRESOLVER_NAME_ERROR.getName());
        }
        //把表达式的前后缀去掉，还原beanName
        String beanName = entry.getValue().substring(3, entry.getValue().length() - 1);
        //如果容器中没有这个bean，抛出异常，否则由spring cloud捕捉到的话，会导致服务不可用
        try {
            Object keySolver = SpringContextUtil.getBean(beanName);
            if (keySolver == null) {
                throw new RouteErrorException(GatewayResultEnum.KEYRESOLVER_NAME_ERROR.getName());
            }
        } catch (Exception e) {
            throw new RouteErrorException(GatewayResultEnum.KEYRESOLVER_NAME_ERROR.getName());
        }
    }

    /**
     * 校验 限流参数是否合法
     *
     * @param entry
     * @throws Exception
     */
    private static void checkTokenArgs(Map.Entry<String, String> entry) throws Exception {
        try {
            if (Integer.parseInt(entry.getValue()) < 1) {
                throw new RouteErrorException(GatewayResultEnum.KEYRESOLVER_LIMIT_ARGS_ERROR.getName());
            }
        } catch (Exception e) {
            throw new RouteErrorException(GatewayResultEnum.KEYRESOLVER_LIMIT_ARGS_ERROR.getName());
        }
    }


    /**
     * 校验重写路径 filter 的参数是否合法
     *
     * @param flter
     * @return
     */
    public static void checkRewritePathArgs(GatewayFilterDefinition flter) throws Exception {
        Map<String, String> args = flter.getArgs();
        if (CollectionUtil.isEmpty(args)) {
            throw new RouteErrorException(GatewayResultEnum.FILTER_NAME_ERROR.getName());
        }
        //判断是否包含了必要的2个参数，重写前url，重写后url
        if (!args.containsKey(regexpKey) || !args.containsKey(replacementKey)) {
            throw new RouteErrorException(GatewayResultEnum.FILTER_ARGS_EMPTY.getName());
        }

        Iterator<Map.Entry<String, String>> iterator = args.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            // 是否配置了 重写前url，重写后url
            //把这 2 个以外的参数移除，防止随便乱填
            if (!regexpKey.equals(entry.getKey()) && !replacementKey.equals(entry.getKey())) {
                iterator.remove();
            }
        }
    }


}
