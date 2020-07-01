package com.youxin.gateway.service;

import com.alibaba.fastjson.JSON;
import com.youxin.gateway.enums.GatewayResultEnum;
import com.youxin.gateway.loadBalancer.EventListener;
import com.youxin.gateway.loadBalancer.ServiceUpdateEvent;
import com.youxin.gateway.util.ResultUtil;
import com.youxin.gateway.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CopyOnWriteArraySet;

@Service
public class NotificationServiceImpl {
    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final CopyOnWriteArraySet<EventListener> eventListeners = new CopyOnWriteArraySet<>();

    /**
     * 注册监听器
     *
     * @param
     * @return
     */
    public boolean registerEventListener(EventListener eventListener) {
        return eventListeners.add(eventListener);
    }

    /**
     * 移除监听器
     *
     * @param
     * @return
     */
    public boolean unregisterEventListener(EventListener eventListener) {
        return eventListeners.remove(eventListener);
    }

    /**
     * 发布服务更新事件
     *
     * @param
     * @return
     */
    public Result<String> publishServiceUpdateEvent(String serviceName) {
        try {
            for (EventListener listener : eventListeners) {
                try {
                    if (listener.getServiceName().equals(serviceName)) {
                        listener.onEvent(new ServiceUpdateEvent(serviceName));
                        break;
                    }
                } catch (Exception e) {
                    logger.error("发布服务更新事件错误: serviceName:{}", serviceName, e);
                }
            }
            logger.info("发布服务更新成功: serviceName:{}", serviceName);
        } catch (Exception e) {
            logger.error("发布服务更新事件错误:serviceName:{}", serviceName, e);
            return ResultUtil.fail(GatewayResultEnum.NOTIFY_SERVICE_FAIL);
        }
        return ResultUtil.success(GatewayResultEnum.NOTIFY_SERVICE_SUCCESS);
    }

}
