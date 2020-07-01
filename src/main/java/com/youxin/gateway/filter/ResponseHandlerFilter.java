package com.youxin.gateway.filter;

import io.netty.util.ReferenceCountUtil;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;


/**
 * 处理响应的 的filter
 *
 * @author huangting
 */
@Component
public class ResponseHandlerFilter implements GlobalFilter, Ordered {
    private static final Logger logger = LoggerFactory.getLogger(ResponseHandlerFilter.class);
    private static final String START_TIME = "startTime";


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();

        //执行完成后 进行调用耗时埋点
        exchange.getAttributes().put(START_TIME, System.currentTimeMillis());

        //原始响应类
        ServerHttpResponse originalResponse = exchange.getResponse();
        DataBufferFactory bufferFactory = originalResponse.bufferFactory();

        //初始化一个 默认的 responseBody
        AtomicReference<String> responseBody = new AtomicReference<>("no-responseBody");

        //重新包装的响应类
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                Flux<? extends DataBuffer> fluxBody = Flux.from(body);
                return super.writeWith(fluxBody.buffer().map(dataBuffer -> {
                    //如果响应过大，会进行截断，出现乱码，然后看api DefaultDataBufferFactory有个join方法可以合并所有的流，乱码的问题解决
                    DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
                    DataBuffer join = dataBufferFactory.join(dataBuffer);
                    byte[] content = new byte[join.readableByteCount()];
                    join.read(content);
                    //释放掉内存
//                    DataBufferUtils.release(join);
                    ReferenceCountUtil.release(join);

                    //如果有返回值，将 responseBody 覆盖
                    responseBody.set(new String(content, StandardCharsets.UTF_8));

                    return bufferFactory.wrap(content);
                }));
            }
        };

        return chain.filter(exchange.mutate().response(decoratedResponse).build())
                .then(Mono.fromRunnable(() -> {
                    //打印响应日志
                    logResponse(exchange, responseBody.get());

                    Long startTime = exchange.getAttribute(START_TIME);
                    if (startTime != null) {
                        Long executeTime = (System.currentTimeMillis() - startTime);
                        //influxDB埋点
//                        metricService.pointRequestLatency(null, request.getURI().getPath(), executeTime);
                    }
                }));
    }


    /**
     * 打印响应报文
     *
     * @param exchange
     */
    public void logResponse(ServerWebExchange exchange, String response) {
        ServerHttpRequest request = exchange.getRequest();
        logger.info("响应报文 URL:{},Method:{},headers:{},response:{}", request.getURI().getPath(), request.getMethod(), exchange.getResponse().getHeaders(), response);
    }


    @Override
    public int getOrder() {
        return -3;
    }
}

