package com.youxin.gateway;

import com.youxin.gateway.config.DefaultRibbonConfig;
import com.youxin.gateway.config.TestRibbonConfig;
import com.youxin.gateway.config.TestRibbonConfig3;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@EnableFeignClients
@RibbonClients(value = {@RibbonClient(value = "test", configuration = TestRibbonConfig.class)
,@RibbonClient(value = "test3", configuration = TestRibbonConfig3.class)
}, defaultConfiguration = DefaultRibbonConfig.class)
@SpringBootApplication
public class DemogatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemogatewayApplication.class, args);
    }

    @Bean
    public RestTemplate callBackRestTemplate() {
        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        httpRequestFactory.setConnectionRequestTimeout(5000);
        httpRequestFactory.setConnectTimeout(10000);
        httpRequestFactory.setReadTimeout(5000);
        return new RestTemplate(httpRequestFactory);
    }
}
