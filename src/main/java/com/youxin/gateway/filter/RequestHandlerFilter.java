package com.youxin.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.youxin.gateway.enums.FilterNameEnum;
import com.youxin.gateway.enums.PredicateNameEnum;
import com.youxin.gateway.model.RequestMessage;
import com.youxin.gateway.service.DefaultPathContainer;
import com.youxin.gateway.service.DynamicRouteServiceImpl;
import com.youxin.gateway.util.CollectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.*;
import org.springframework.cloud.gateway.filter.factory.RewritePathGatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

/**
 * 参照 ModifyRequestBodyGatewayFilterFactory 写的一个处理 requestBody的filter
 *
 * @author huangting
 */
@Component
public class RequestHandlerFilter implements GlobalFilter, Ordered {
    private static final Logger logger = LoggerFactory.getLogger(RequestHandlerFilter.class);

    private static final String METHOD_POST = "POST";
    private static final String METHOD_GET = "GET";


    @Autowired
    private DynamicRouteServiceImpl dynamicRouteService;



    @Autowired
    private RouteLocator routeLocator;


    @Override
    public Mono filter(ServerWebExchange exchange, GatewayFilterChain chain) {


        ServerHttpRequest request = exchange.getRequest();
        //POST和 GET 的处理逻辑不一样
        if (METHOD_POST.equals(exchange.getRequest().getMethodValue())) {
            ServerRequest serverRequest = new DefaultServerRequest(exchange);

            Mono<String> modifiedBody = serverRequest.bodyToMono(String.class)
                    .switchIfEmpty(Mono.just("no-requestBody"))
                    .flatMap(requestBody -> {
                        //打印请求报文
                        logRequestLog(exchange, requestBody);

                        return Mono.just(requestBody);
                    });

            BodyInserter bodyInserter = BodyInserters.fromPublisher(modifiedBody, String.class);
            HttpHeaders headers = new HttpHeaders();
            headers.putAll(exchange.getRequest().getHeaders());

            // the new content type will be computed by bodyInserter
            // and then set in the request decorator
            headers.remove(HttpHeaders.CONTENT_LENGTH);

            CachedBodyOutputMessage outputMessage = new CachedBodyOutputMessage(exchange, headers);
            return bodyInserter.insert(outputMessage, new BodyInserterContext())
                    // .log("modify_request", Level.INFO)
                    .then(Mono.defer(() -> {
                        ServerHttpRequest decorator = decorate(exchange, headers, outputMessage);
                        return chain.filter(exchange.mutate().request(decorator).build());
                    }));

        } else {
            //打印请求报文
            logRequestLog(exchange, null);
            chain.filter(exchange);
        }
        return chain.filter(exchange);
    }


    ServerHttpRequestDecorator decorate(ServerWebExchange exchange, HttpHeaders headers, CachedBodyOutputMessage outputMessage) {
        return new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            public HttpHeaders getHeaders() {
                long contentLength = headers.getContentLength();
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.putAll(super.getHeaders());
                if (contentLength > 0) {
                    httpHeaders.setContentLength(contentLength);
                } else {
                    // TODO: this causes a 'HTTP/1.1 411 Length Required' // on
                    // httpbin.org
                    httpHeaders.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
                }
                return httpHeaders;
            }

            @Override
            public Flux<DataBuffer> getBody() {
                return outputMessage.getBody();
            }
        };
    }


    /**
     * 打印请求日志
     *
     * @param
     */
    public void logRequestLog(ServerWebExchange exchange, String requestBody) {

        ServerHttpRequest request = exchange.getRequest();
        String requestParam = request.getQueryParams().toString();
        logger.info("请求报文 URL:{},Method:{},headers:{},param:{},requestbody:{}", request.getURI().getPath(), request.getMethod(), request.getHeaders(), requestParam, requestBody);

    }

    /**
     * 获取匹配的路由信息
     */
    private Route getMatchRoute(ServerWebExchange exchange) {

        final Route[] result = {null};
        routeLocator.getRoutes().concatMap(route -> Mono.just(route).filterWhen(r ->
                r.getPredicate().apply(exchange)
        )).subscribe(r -> result[0] = r);
        return result[0];
    }

    /**
     * 获取匹配的路由信息
     */
    private RouteDefinition getMatchRouteDefinition(ServerHttpRequest request) {

        RouteDefinition result = null;
        String uri = request.getURI().getPath();
        List<RouteDefinition> list = dynamicRouteService.list();


        if (CollectionUtil.isNotEmpty(list)) {

            Optional<RouteDefinition> matchRouteDefinition = list.stream().filter(routeDefinition ->
                    routeDefinition.getPredicates().stream()
                            .filter(predicate -> PredicateNameEnum.PATH.getName().equals(predicate.getName()))
                            .anyMatch(predicate -> new PathPatternParser().parse(predicate.getArgs().get("pattern")).matches(DefaultPathContainer.createFromUrlPath(uri)))
            ).findFirst();

            if (matchRouteDefinition.isPresent()) {
                result = matchRouteDefinition.get();
            }

        }

        return result;
    }


    @Override
    public int getOrder() {
        return -3;
    }
}

