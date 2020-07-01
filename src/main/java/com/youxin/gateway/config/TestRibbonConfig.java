package com.youxin.gateway.config;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.*;
import com.youxin.gateway.loadBalancer.TestServerList;
import com.youxin.gateway.loadBalancer.YouxinNotificationServerListUpdater;
import com.youxin.gateway.service.FetchListServiceImpl;
import com.youxin.gateway.service.NotificationServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;


public class TestRibbonConfig {

    @Primary
    @Bean()
    public IRule randomRibbonRule() {
        return new RandomRule();
    }

    @Autowired
    private SpringClientFactory factory;

    @Primary
    @Bean(name="testServiceList")
    public ServerList<Server> ribbonServerList(FetchListServiceImpl fetchListService, IClientConfig clientConfig) {
        return new TestServerList(fetchListService, clientConfig);
    }

    /**
     * 服务更新通知机制
     * @param notificationService
     * @param clientConfig
     * @return
     */
    @Primary
    @Bean(name="testServiceListUpdeter")
    public ServerListUpdater ribbonServerListUpdater(NotificationServiceImpl notificationService, IClientConfig clientConfig) {
        return new YouxinNotificationServerListUpdater(notificationService, clientConfig);
    }
}
