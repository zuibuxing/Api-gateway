package com.youxin.gateway.loadBalancer;


public abstract class EventListener {

    String serviceName;

    public EventListener(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceName(){
        return serviceName;
    }

    public void onEvent(Event event) {
    }

    @Override
    public boolean equals(Object obj) {
        if (null == obj) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (obj instanceof EventListener) {
            EventListener eventListener = (EventListener) obj;
            return serviceName.equals(eventListener.serviceName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return serviceName.hashCode();
    }

}
