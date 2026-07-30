package com.wyj.kgc.service;

public interface VerificationCodeSender {

    void send(String target, String code);
}
