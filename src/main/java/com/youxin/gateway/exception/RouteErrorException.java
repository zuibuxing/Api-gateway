package com.youxin.gateway.exception;

import lombok.Data;

@Data
public class RouteErrorException extends Exception {

    private Exception e;

    /**
     * serialVersionUID:TODO(用一句话描述这个变量表示什么).
     *
     * @since JDK 1.7
     */
    private static final long serialVersionUID = 1L;

    public RouteErrorException(String message) {
        super(message);
    }

    public RouteErrorException(Exception e) {
        this.e = e;
    }
}
