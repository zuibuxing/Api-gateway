package com.youxin.gateway.loadBalancer;


public class ServiceUpdateEvent extends Event {

    public ServiceUpdateEvent(String eventName) {
        super();
        setEventName(eventName);
    }

    @Override
    public boolean equals(Object obj) {
        if (null == obj) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (obj instanceof ServiceUpdateEvent) {
            ServiceUpdateEvent serviceUpdateEvent = (ServiceUpdateEvent) obj;
            return eventName.equals(serviceUpdateEvent.eventName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return eventName.hashCode();
    }

}
