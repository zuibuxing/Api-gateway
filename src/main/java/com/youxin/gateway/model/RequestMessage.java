package com.youxin.gateway.model;

import lombok.Data;

import java.util.Date;

@Data
public class RequestMessage {
    private String url;
    private String uri;
    private String serviceName;
    private String method;
    private String header;
    private String requestParam;
    private String requestBody;
    private Date createTime;


}
