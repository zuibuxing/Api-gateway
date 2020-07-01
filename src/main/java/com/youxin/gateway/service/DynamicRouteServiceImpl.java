package com.youxin.gateway.service;

import com.alibaba.fastjson.JSON;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import com.youxin.gateway.enums.FilterNameEnum;
import com.youxin.gateway.enums.GatewayResultEnum;
import com.youxin.gateway.enums.PredicateNameEnum;
import com.youxin.gateway.exception.RouteErrorException;
import com.youxin.gateway.model.GatewayFilterDefinition;
import com.youxin.gateway.model.GatewayPredicateDefinition;
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
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.*;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sun.plugin2.applet.StopListener;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Service
public class DynamicRouteServiceImpl implements ApplicationEventPublisherAware {
    private static final Logger logger = LoggerFactory.getLogger(DynamicRouteServiceImpl.class);

    @Autowired
    private RouteDefinitionWriter routeDefinitionWriter;

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;


    private ApplicationEventPublisher publisher;

    @Autowired
    private FetchListServiceImpl fetchListService;



    /**
     * 增加路由
     *
     * @param gwdefinition
     * @return
     */
    public Result<String> add(GatewayRouteDefinition gwdefinition) {
        try {
            RouteDefinition definition = assembleRouteDefinition(gwdefinition);
            routeDefinitionWriter.save(Mono.just(definition)).subscribe();
            this.publisher.publishEvent(new RefreshRoutesEvent(this));

            return ResultUtil.success(GatewayResultEnum.ADD_ROUTE_SUCCESS);
        } catch (RouteErrorException e) {
            logger.error("添加路由失败 definition:{}", JSON.toJSONString(gwdefinition), e);
            return ResultUtil.fail(e.getMessage(), JSON.toJSONString(gwdefinition));
        } catch (Exception e) {
            logger.error("添加路由失败 definition:{}", JSON.toJSONString(gwdefinition), e);
            return ResultUtil.fail("未知错误");
        }
    }

    /**
     * 批量增加路由
     *
     * @param gatewayRouteDefinitionList
     * @return
     */
    public Result<?> addBatch(List<GatewayRouteDefinition> gatewayRouteDefinitionList) {
        try {
            List<Result> list = new ArrayList<>();
            gatewayRouteDefinitionList.stream().forEach(route -> list.add(add(route)));
            return ResultUtil.success(GatewayResultEnum.ADD_ROUTE_SUCCESS, list);
        } catch (Exception e) {
            logger.error("批量添加路由失败 definition:{}", JSON.toJSONString(gatewayRouteDefinitionList), e);
            return ResultUtil.fail("未知错误");
        }
    }


    /**
     * 更新路由
     *
     * @param gwdefinition
     * @return
     */
    public Result<String> update(GatewayRouteDefinition gwdefinition) {
        RouteDefinition definition;
        try {
            definition = assembleRouteDefinition(gwdefinition);
            this.routeDefinitionWriter.delete(Mono.just(definition.getId()));
        } catch (Exception e) {
            logger.error("更新路由失败 definition:{}", JSON.toJSONString(gwdefinition), e);
            return ResultUtil.fail("更新失败,路由信息不存在,routeId : gwdefinition.getId()");
        }
        try {
            routeDefinitionWriter.save(Mono.just(definition)).subscribe();
            this.publisher.publishEvent(new RefreshRoutesEvent(this));
            return ResultUtil.success(GatewayResultEnum.UPDATE_ROUTE_SUCCESS);
        } catch (Exception e) {
            logger.error("更新路由失败 definition:{}", JSON.toJSONString(gwdefinition), e);
            return ResultUtil.fail("未知错误");
        }


    }

    /**
     * 删除路由
     *
     * @param id
     * @return
     */
    public Result<String> delete(String id) {
        try {
            this.routeDefinitionWriter.delete(Mono.just(id)).subscribe();
            this.publisher.publishEvent(new RefreshRoutesEvent(this));
            return ResultUtil.success(GatewayResultEnum.DELETE_ROUTE_SUCCESS);
        } catch (Exception e) {
            logger.error("更新路由失败 routId:{}", id, e);
            return ResultUtil.fail("未知错误");
        }

    }

    /**
     * 刷新
     *
     * @return
     */
    public Result<?> refresh(String lastUpdateTime, String status) {
        try {
            List<Result> resultlist = new ArrayList<>();
            List<GatewayRouteDefinition> routeList = fetchListService.fetchRouteList(lastUpdateTime, status);
            if (null == routeList) {
                return ResultUtil.fail(GatewayResultEnum.REFRESH_ROUTE_FALSE);
            }
            routeList.stream().forEach(route -> {
                if (1 == route.getStatus()) {
                    resultlist.add(add(route));
                } else if (0 == route.getStatus()) {
                    resultlist.add(delete(route.getId()));
                }
            });
            return ResultUtil.success(GatewayResultEnum.REFRESH_ROUTE_SUCCESS, resultlist);
        } catch (Exception e) {
            logger.error("重新加载路由失败: ", e);
            return ResultUtil.fail("未知错误");
        }
    }

    /**
     * 路由列表
     *
     * @return
     */
    public List<RouteDefinition> list() {
        try {
            List<RouteDefinition> list = new ArrayList<>();
            this.routeDefinitionLocator.getRouteDefinitions().sort(Comparator.comparing(RouteDefinition::getOrder)).subscribe(route -> list.add(route));
            return list;
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * 刷新
     *
     * @return
     */
    public void refreshCache() {
        try {
            this.publisher.publishEvent(new RefreshRoutesEvent(this));

        } catch (Exception e) {
        }

    }


    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }


    /**
     * 将自定义路由组装为springcloud路由
     *
     * @param gwdefinition
     * @return
     */
    private RouteDefinition assembleRouteDefinition(GatewayRouteDefinition gwdefinition) throws Exception {

        //检验上传的路由信息中断言信息是否合法
        checkPredicate(gwdefinition);

        //检验上传的路由信息中filter信息是否合法
        checkFliter(gwdefinition);

        RouteDefinition definition = new RouteDefinition();

        // ID
        definition.setId(gwdefinition.getId());

        // Predicates
        List<PredicateDefinition> pdList = new ArrayList<>();
        for (GatewayPredicateDefinition gpDefinition : gwdefinition.getPredicates()) {
            PredicateDefinition predicate = new PredicateDefinition();
            predicate.setArgs(gpDefinition.getArgs());
            predicate.setName(gpDefinition.getName());
            pdList.add(predicate);
        }
        definition.setPredicates(pdList);

        // Filters
        List<FilterDefinition> fdList = new ArrayList<>();
        for (GatewayFilterDefinition gfDefinition : gwdefinition.getFilters()) {
            FilterDefinition filter = new FilterDefinition();
            filter.setArgs(gfDefinition.getArgs());
            filter.setName(gfDefinition.getName());
            fdList.add(filter);
        }
        definition.setFilters(fdList);

        // URI
        //将 "_" 替换为 "-" ，防止 JDK 的URL解析BUG
        URI uri = UriComponentsBuilder.fromUriString(gwdefinition.getUri().replaceAll("_", "-")).build().toUri();
        definition.setUri(uri);

        // Order
        definition.setOrder(gwdefinition.getOrder());

        return definition;
    }


    /**
     * 检验上传的路由信息中的断言信息是否合法
     *
     * @param gwdefinition
     * @throws Exception
     */
    public void checkPredicate(GatewayRouteDefinition gwdefinition) throws Exception {
        //若未配置断言器，抛出异常
        if (CollectionUtil.isEmpty(gwdefinition.getPredicates())) {
            throw new RouteErrorException(GatewayResultEnum.PREDICATE_EMPTY.getName());
        }

        for (GatewayPredicateDefinition predicate : gwdefinition.getPredicates()) {
            //路由的断言器中有任意一个的名字不符合规范，则抛出异常
            if (!PredicateNameEnum.checkPredicateName(predicate.getName())) {
                throw new RouteErrorException(GatewayResultEnum.PREDICATE_NAME_ERROR.getName());
            }
        }
    }


    /**
     * 检验上传的路由信息中的filter信息是否 合法
     *
     * @param gwdefinition
     * @throws Exception
     */
    public void checkFliter(GatewayRouteDefinition gwdefinition) throws Exception {


        for (GatewayFilterDefinition flter : gwdefinition.getFilters()) {
            //路由的过滤器中有任意一个的名字不符合规范，则抛出异常
            if (!FilterNameEnum.checkFilterName(flter.getName())) {
                throw new RouteErrorException(GatewayResultEnum.FILTER_NAME_ERROR.getName());
            }

            //校验限流filter的参数是否合法
            if (FilterNameEnum.REQUESTRATELIMITER.getName().equals(flter.getName())) {
                FilterNameEnum.checkRequestLimitArgs(flter);
            }

            //校验重写路径 filter 的参数是否合法
            if (FilterNameEnum.REWRITEPATH.getName().equals(flter.getName())) {
                FilterNameEnum.checkRewritePathArgs(flter);
            }


        }
    }


}
