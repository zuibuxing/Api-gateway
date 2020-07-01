package com.youxin.gateway.service;


import com.netflix.loadbalancer.Server;
import com.youxin.gateway.loadBalancer.EventListener;
import com.youxin.gateway.model.GatewayRouteDefinition;
import com.youxin.gateway.util.CollectionUtil;
import com.youxin.gateway.util.HttpClientUtils;
import com.youxin.gateway.util.ResultUtil;
import com.youxin.gateway.result.Result;
import com.youxin.gateway.result.ResultCodeEnum;
import com.youxin.servitization.meta.ServiceMetaDataVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

@Service
public class FetchListServiceImpl {

    private static final Logger logger = LoggerFactory.getLogger(FetchListServiceImpl.class);

    private final CopyOnWriteArraySet<EventListener> eventListeners = new CopyOnWriteArraySet<>();

    private static final String FETCH_ROUTE_METHOD = "/route/list";

    private static final String FETCH_SERVER_METHOD = "/metadata/getMetaData";

    @Autowired
    private HttpClientUtils httpClientUtils;

    @Value("${youxin.service.fetchRegisterUrl}")
    private String fetchRegisterUrl;

    @Value("${youxin.service.retryTime}")
    private int retryTime;



    /**
     * 拉取路由列表
     *
     * @return routeList
     */
    public List<GatewayRouteDefinition> fetchRouteList(String lastUpdateTime, String status) {
        List<GatewayRouteDefinition> routeList = null;
        List<String> fetchUrls = new ArrayList<>(Arrays.asList(fetchRegisterUrl.split(",")));
        String fetchUrl;
        String requestUrl = "";
        //获取的时候进行重试
        for (int retry = 0; retry < retryTime; retry++) {
            if (retry >= fetchUrls.size()) {
                throw new RuntimeException("Cannot getServiceList request on any known server");
            }
            fetchUrl = fetchUrls.get(retry);
            try {
                requestUrl = fetchUrl + FETCH_ROUTE_METHOD;
                Map<String, Object> param = new HashMap<>();
                param.put("lastUpdateTime", lastUpdateTime);
                param.put("status", status);
                ResponseEntity<Result> responseEntity = httpClientUtils.getForObject(requestUrl, param, new ParameterizedTypeReference<Result<List<GatewayRouteDefinition>>>() {
                });
                Result result = responseEntity.getBody();
                if (ResultUtil.resultIsAvailabe(result)) {
                    routeList = (List<GatewayRouteDefinition>) result.getData();
                    return routeList;
                }
            } catch (Exception e) {
                logger.error("拉取路由列表失败,fetchUrl:{}", requestUrl, e);
            }
        }
        return routeList;
    }


    /**
     * 拉取服务列表
     *
     * @param serviceId 服务名
     * @return serverList
     */
    public List<Server> fetchServerList(String serviceId) {
        List<Server> serverList = null;
        List<String> fetchUrls = new ArrayList<>(Arrays.asList(fetchRegisterUrl.split(",")));
        String fetchUrl;
        String requestUrl = "";
        //获取的时候进行重试
        for (int retry = 0; retry < retryTime; retry++) {
            serverList = new ArrayList<>();
            if (retry >= fetchUrls.size()) {
                throw new RuntimeException("Cannot getServiceList request on any known server");
            }

            fetchUrl = fetchUrls.get(retry);
            try {
                requestUrl = fetchUrl + FETCH_SERVER_METHOD;
                Map<String, String> param = new HashMap<>();
                param.put("serviceId", serviceId);
                ResponseEntity<Result> responseEntity = httpClientUtils.getForObject(requestUrl, param, new ParameterizedTypeReference<Result<List<ServiceMetaDataVO>>>() {
                });
                Result result = responseEntity.getBody();
                if (ResultUtil.resultIsAvailabe(result)) {
                    List<ServiceMetaDataVO> serviceMetaDataVoList = (List<ServiceMetaDataVO>) result.getData();
                    if (CollectionUtil.isNotEmpty(serviceMetaDataVoList)) {
//                        logger.info("从 {} 拉取 {} 服务，返回列表: {}", fetchUrl, serviceId, JSON.toJSONString(serviceList));
                        for (ServiceMetaDataVO serviceMetaDataVO : serviceMetaDataVoList) {
                            serverList.add(createServer(serviceMetaDataVO));
                        }
                        return serverList;
                    }
                }
            } catch (Exception e) {
                logger.error("拉取服务列表失败,fetchUrl:{},serviceId:{}", requestUrl, serviceId, e);
            }
        }
        return serverList;
    }



    /**
     * 根据meteServer 创建Server类
     *
     * @param serviceMetaDataVO
     * @return Server
     */
    private Server createServer(ServiceMetaDataVO serviceMetaDataVO) {
        return new Server(serviceMetaDataVO.getIp(), serviceMetaDataVO.getPort());
    }

}
