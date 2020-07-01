package com.youxin.gateway.loadBalancer;

public abstract class Event {
    String eventName = null;

    void setEventName(String eventName) {
        this.eventName = eventName;
    }

    String getEventName() {
        return eventName;
    }

}
