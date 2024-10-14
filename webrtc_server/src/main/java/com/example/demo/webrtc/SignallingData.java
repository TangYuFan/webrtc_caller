package com.example.demo.webrtc;

import java.util.Map;

public class SignallingData {
    String eventName;
    Map<String, Object> data;

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "Data{" +
                "eventName='" + eventName + '\'' +
//                ", data=" + data +
                '}';
    }
}
