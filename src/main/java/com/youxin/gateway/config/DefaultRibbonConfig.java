package com.youxin.gateway.config;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.*;
import com.youxin.gateway.loadBalancer.YouxinNotificationServerListUpdater;
import com.youxin.gateway.loadBalancer.YouxinServerList;
import com.youxin.gateway.service.FetchListServiceImpl;
import com.youxin.gateway.service.NotificationServiceImpl;
import org.springframework.context.annotation.Bean;


public class DefaultRibbonConfig {

    @Bean
    public IRule ribbonRule() {
        return new RoundRobinRule();
    }

    @Bean
    public ServerList<Server> ribbonServerList(FetchListServiceImpl fetchListService, IClientConfig clientConfig) {
        return new YouxinServerList(fetchListService, clientConfig);
    }


    /**
     * 服务更新通知机制
     * @param notificationService
     * @param clientConfig
     * @return
     */
    @Bean
    public ServerListUpdater ribbonServerListUpdater(NotificationServiceImpl notificationService, IClientConfig clientConfig) {
        return new YouxinNotificationServerListUpdater(notificationService, clientConfig);
    }
}
