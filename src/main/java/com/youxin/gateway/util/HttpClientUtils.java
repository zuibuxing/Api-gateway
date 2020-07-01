package com.youxin.gateway.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * @author huangting
 */
@Component
public class HttpClientUtils {
    private static Logger logger = LoggerFactory.getLogger(HttpClientUtils.class);

    @Autowired
    private RestTemplate restTemplate;

    public <T> ResponseEntity<T> getForObject(String url, Object request, Class<T> responseType) {
        HttpEntity<Object> entity = new HttpEntity<>(request);
        Map<String, String> requestMap = (Map<String, String>) request;
        StringBuilder urlBuilder = new StringBuilder(url).append("?");
        for (String key : requestMap.keySet()) {
            if (urlBuilder.toString().endsWith("?")) {
                urlBuilder.append(key).append("=").append(requestMap.get(key));
            } else {
                urlBuilder.append("&").append(key).append("=").append(requestMap.get(key));
            }
        }
        url = urlBuilder.toString();
//        logger.info("====get--url===" + url);
        ResponseEntity<T> response = this.restTemplate.exchange(url, HttpMethod.GET, entity, responseType);
        if (response.getStatusCode() == HttpStatus.OK) {
            return response;
        } else if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            return null;
        }
        return response;
    }

    /**
     * 支持对象类型的返回值
     *
     * @param url
     * @param request
     * @param parameterizedTypeReference
     * @param <T>
     * @return
     */
    public <T> ResponseEntity<T> getForObject(String url, Object request, ParameterizedTypeReference parameterizedTypeReference) {
        HttpEntity<Object> entity = new HttpEntity<>(request);
        StringBuilder urlBuilder = new StringBuilder(url);
        if (null != request) {
            Map<String, String> requestMap = (Map<String, String>) request;
            urlBuilder = new StringBuilder(url).append("?");
            for (String key : requestMap.keySet()) {
                if (urlBuilder.toString().endsWith("?")) {
                    urlBuilder.append(key).append("=").append(requestMap.get(key));
                } else {
                    urlBuilder.append("&").append(key).append("=").append(requestMap.get(key));
                }
            }
        }
        url = urlBuilder.toString();
//        logger.info("====get--url===" + url);
//        ResponseEntity<T> response = this.restTemplate.exchange(url, HttpMethod.GET, entity, type);
        ResponseEntity<T> response = this.restTemplate.exchange(url, HttpMethod.GET, entity, parameterizedTypeReference);
        if (response.getStatusCode() == HttpStatus.OK) {
            return response;
        } else if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            return null;
        }
        return response;
    }


    public <T> ResponseEntity<T> postForObject(String url, Object request, Class<T> responseType) {
        // set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Content-Type", "application/json");
        HttpEntity<Object> entity = new HttpEntity<>(request, headers);

        // send request and parse result
//        logger.info("====post--url===" + url + ", header:" + headers);
        Map<?, ?> paras = (Map<?, ?>) request;
        ResponseEntity<T> response = this.restTemplate.exchange(url, HttpMethod.POST, entity, responseType);

        if (response.getStatusCode() == HttpStatus.OK) {
            return response;
        } else if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            return null;
        }
        return response;
    }

}
