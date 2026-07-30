package com.wyj.kgc.config;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TencentSmsConfig {

    @Bean
    public SmsClient smsClient(VerificationCodeProperties properties) {
        Credential credential = new Credential(
                properties.getSms().getSecretId(),
                properties.getSms().getSecretKey());
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint("sms.tencentcloudapi.com");
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);
        return new SmsClient(credential, properties.getSms().getRegion(), clientProfile);
    }
}
