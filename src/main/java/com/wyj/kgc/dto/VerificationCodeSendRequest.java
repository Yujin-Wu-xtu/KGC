package com.wyj.kgc.dto;

public class VerificationCodeSendRequest {

    private String channel;
    private String target;

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }
}
