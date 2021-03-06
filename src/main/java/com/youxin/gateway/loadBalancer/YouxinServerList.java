package com.youxin.gateway.loadBalancer;

//import com.netflix.appinfo.InstanceInfo;

import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.client.config.IClientConfigKey;
import com.netflix.config.ConfigurationManager;
import com.netflix.loadbalancer.AbstractServerList;
import com.netflix.loadbalancer.Server;
import com.youxin.gateway.service.FetchListServiceImpl;
import com.youxin.gateway.util.CollectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

//import com.netflix.discovery.EurekaClient;

public class YouxinServerList extends AbstractServerList<Server> {

    private static final Logger logger = LoggerFactory.getLogger(YouxinServerList.class);

    List<Server> serverList = new ArrayList<>();

    private FetchListServiceImpl fetchListService;

    private String clientName;
    private String vipAddresses;
    private boolean isSecure = false;

    private boolean prioritizeVipAddressBasedServers = true;

    private String datacenter;
    private String targetRegion;

    private int overridePort = DefaultClientConfigImpl.DEFAULT_PORT;
    private boolean shouldUseOverridePort = false;
    private boolean shouldUseIpAddr = false;


    public YouxinServerList(FetchListServiceImpl fetchListService, IClientConfig clientConfig) {
        initWithNiwsConfig(clientConfig);
        this.fetchListService = fetchListService;
    }

    @Override
    public void initWithNiwsConfig(IClientConfig clientConfig) {
        clientName = clientConfig.getClientName();
        vipAddresses = clientConfig.resolveDeploymentContextbasedVipAddresses();

        isSecure = Boolean.parseBoolean("" + clientConfig.getProperty(CommonClientConfigKey.IsSecure, "false"));
        prioritizeVipAddressBasedServers = Boolean.parseBoolean("" + clientConfig.getProperty(CommonClientConfigKey.PrioritizeVipAddressBasedServers, prioritizeVipAddressBasedServers));
        datacenter = ConfigurationManager.getDeploymentContext().getDeploymentDatacenter();
        targetRegion = (String) clientConfig.getProperty(CommonClientConfigKey.TargetRegion);

        shouldUseIpAddr = clientConfig.getPropertyAsBoolean(CommonClientConfigKey.UseIPAddrForServer, DefaultClientConfigImpl.DEFAULT_USEIPADDRESS_FOR_SERVER);

        // override client configuration and use client-defined port
        if (clientConfig.getPropertyAsBoolean(CommonClientConfigKey.ForceClientPortConfiguration, false)) {

            if (isSecure) {

                if (clientConfig.containsProperty(CommonClientConfigKey.SecurePort)) {

                    overridePort = clientConfig.getPropertyAsInteger(CommonClientConfigKey.SecurePort, DefaultClientConfigImpl.DEFAULT_PORT);
                    shouldUseOverridePort = true;

                } else {
                    logger.warn(clientName + " set to force client port but no secure port is set, so ignoring");
                }
            } else {

                if (clientConfig.containsProperty(CommonClientConfigKey.Port)) {

                    overridePort = clientConfig.getPropertyAsInteger(CommonClientConfigKey.Port, DefaultClientConfigImpl.DEFAULT_PORT);
                    shouldUseOverridePort = true;

                } else {
                    logger.warn(clientName + " set to force client port but no port is set, so ignoring");
                }
            }
        }
    }

    @Override
    public List<Server> getInitialListOfServers() {
        List<Server> fetchServerList = obtainServersViaDiscovery();
        //拉取到列表才更新，防止注册中心宕掉无法访问服务
        if (CollectionUtil.isNotEmpty(fetchServerList)) {
            serverList = fetchServerList;
        }
        return serverList;
    }

    @Override
    public List<Server> getUpdatedListOfServers() {
        List<Server> fetchServerList = obtainServersViaDiscovery();
        //拉取到列表才更新，防止注册中心宕掉无法访问服务
        if (CollectionUtil.isNotEmpty(fetchServerList)) {
            serverList = fetchServerList;
        }
        return serverList;
    }

    private List<Server> obtainServersViaDiscovery() {
//        List<Server> serverList = fetchListService.fetchServerList(clientName);

        List<Server> serverList =new ArrayList<>();
        serverList.add(new Server("localhost",81));
        serverList.add(new Server("localhost",82));
        serverList.add(new Server("localhost",83));
        return serverList;
    }


    public String getVipAddresses() {
        return vipAddresses;
    }

    public void setVipAddresses(String vipAddresses) {
        this.vipAddresses = vipAddresses;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("YouxinServerList:");
        sb.append("; clientName:").append(clientName);
        sb.append("; Effective vipAddresses:").append(vipAddresses);
        sb.append("; isSecure:").append(isSecure);
        sb.append("; datacenter:").append(datacenter);
        return sb.toString();
    }


    private static IClientConfig createClientConfig(String vipAddresses) {
        IClientConfig clientConfig = DefaultClientConfigImpl.getClientConfigWithDefaultValues();
        clientConfig.set(IClientConfigKey.Keys.DeploymentContextBasedVipAddresses, vipAddresses);
        return clientConfig;
    }
}

