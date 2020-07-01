package com.youxin.gateway.error;

import com.youxin.gateway.filter.CachedBodyOutputMessage;
import com.youxin.gateway.filter.DefaultServerRequest;
import com.youxin.gateway.util.IpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 全局错误处理器
 *
 * @author huangting
 */
public class GlobalErrorExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalErrorExceptionHandler.class);

    private static final String METHOD_POST = "POST";

    /**
     * MessageReader
     */
    private List<HttpMessageReader<?>> messageReaders = Collections.emptyList();

    /**
     * MessageWriter
     */
    private List<HttpMessageWriter<?>> messageWriters = Collections.emptyList();

    /**
     * ViewResolvers
     */
    private List<ViewResolver> viewResolvers = Collections.emptyList();

    /**
     * 存储处理异常后的信息
     */
    private ThreadLocal<Map<String, Object>> exceptionHandlerResult = new ThreadLocal<>();

    /**
     * 参考AbstractErrorWebExceptionHandler
     */
    public void setMessageReaders(List<HttpMessageReader<?>> messageReaders) {
        Assert.notNull(messageReaders, "'messageReaders' must not be null");
        this.messageReaders = messageReaders;
    }

    /**
     * 参考AbstractErrorWebExceptionHandler
     */
    public void setViewResolvers(List<ViewResolver> viewResolvers) {
        this.viewResolvers = viewResolvers;
    }

    /**
     * 参考AbstractErrorWebExceptionHandler
     */
    public void setMessageWriters(List<HttpMessageWriter<?>> messageWriters) {
        Assert.notNull(messageWriters, "'messageWriters' must not be null");
        this.messageWriters = messageWriters;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        // 按照异常类型进行处理
        HttpStatus httpStatus;
        String body;

        if (HttpStatus.TOO_MANY_REQUESTS == exchange.getResponse().getStatusCode()) {
            httpStatus = HttpStatus.TOO_MANY_REQUESTS;
            body = "请求过多";
            ex = new RuntimeException(body);
        } else if (ex instanceof NotFoundException && ((NotFoundException) ex).getStatus() == HttpStatus.NOT_FOUND || ex instanceof ResponseStatusException && ((ResponseStatusException) ex).getStatus() == HttpStatus.NOT_FOUND) {
            httpStatus = HttpStatus.NOT_FOUND;
//            body = "Service Not Found";
            body = "该接口路由信息尚未配置";
        } else if (ex instanceof ResponseStatusException) {
            ResponseStatusException responseStatusException = (ResponseStatusException) ex;
            httpStatus = responseStatusException.getStatus();
            body = responseStatusException.getMessage();
        } else {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            body = "Internal Server Error";
        }
        //封装响应体,此body可修改为自己的jsonBody
        Map<String, Object> result = new HashMap<>(2, 1);
        result.put("httpStatus", httpStatus);

        String msg = "{\"code\":" + httpStatus + ",\"message\": \"" + body + "\"}";
        result.put("body", msg);
        //错误记录
        ServerHttpRequest request = exchange.getRequest();
        if (HttpStatus.TOO_MANY_REQUESTS == exchange.getResponse().getStatusCode()) {
            logger.error("[请求过多，已经限流] 请求路径:{}, 异常信息:", request.getPath(), ex);
        } else {
            String ip = IpUtil.getRemoteHost(request);
            logger.error("[全局异常处理] 请求路径:{},请求ip:{}, 异常信息:", request.getPath(), ip, ex);
        }


        //如果是接口404，将请求信息记录下来，必要时候可以回放请求
        if (httpStatus == HttpStatus.NOT_FOUND) {
            //POST和 GET 的处理逻辑不一样
            if (METHOD_POST.equals(request.getMethodValue())) {

                ServerRequest serverRequest = new DefaultServerRequest(exchange);
                Mono<String> modifiedBody = serverRequest.bodyToMono(String.class)
                        .switchIfEmpty(Mono.just("no-requestBody"))
                        .flatMap(body2 -> {
                            //打印请求报文
                            logRequestLog(request, body2);
                            return Mono.just(body);
                        });

                exceptionHandlerResult.set(result);
                return modifiedBody
                        .then(Mono.defer(() -> {
                            ServerRequest newRequest = ServerRequest.create(exchange, this.messageReaders);
                            return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse).route(newRequest)
                                    .flatMap((handler) -> handler.handle(newRequest))
                                    .flatMap((response) -> write(exchange, response));
                        }));
            } else {
                logRequestLog(request, null);
            }
        }


        //参考AbstractErrorWebExceptionHandler
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }
        exceptionHandlerResult.set(result);
        ServerRequest newRequest = ServerRequest.create(exchange, this.messageReaders);
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse).route(newRequest)
                .switchIfEmpty(Mono.error(ex))
                .flatMap((handler) -> handler.handle(newRequest))
                .flatMap((response) -> write(exchange, response));

    }

    /**
     * 打印请求日志
     *
     * @param request
     */
    public void logRequestLog(ServerHttpRequest request, String requestBody) {
        String requestParam = request.getQueryParams().toString();
        logger.info("响应错误 URL:{},Method:{},headers:{},param:{},requestbody:{}", request.getURI(), request.getMethod(), request.getHeaders(), requestParam, requestBody);
    }

    /**
     * 参考DefaultErrorWebExceptionHandler
     */
    protected Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Map<String, Object> result = exceptionHandlerResult.get();
        return ServerResponse.status((HttpStatus) result.get("httpStatus"))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .body(BodyInserters.fromObject(result.get("body")));
    }

    /**
     * 参考AbstractErrorWebExceptionHandler
     */
    private Mono<? extends Void> write(ServerWebExchange exchange,
                                       ServerResponse response) {
        exchange.getResponse().getHeaders()
                .setContentType(response.headers().getContentType());
        return response.writeTo(exchange, new ResponseContext());
    }

    /**
     * 参考AbstractErrorWebExceptionHandler
     */
    private class ResponseContext implements ServerResponse.Context {

        @Override
        public List<HttpMessageWriter<?>> messageWriters() {
            return GlobalErrorExceptionHandler.this.messageWriters;
        }

        @Override
        public List<ViewResolver> viewResolvers() {
            return GlobalErrorExceptionHandler.this.viewResolvers;
        }
    }
}
