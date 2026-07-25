package com.wyj.kgc.service;

import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.wyj.kgc.config.VerificationCodeProperties;
import org.springframework.stereotype.Service;

@Service
public class TencentSmsVerificationCodeSender {

    private final SmsClient smsClient;
    private final VerificationCodeProperties properties;

    public TencentSmsVerificationCodeSender(SmsClient smsClient, VerificationCodeProperties properties) {
        this.smsClient = smsClient;
        this.properties = properties;
    }

    public void send(String target, String code) {
        SendSmsRequest request = new SendSmsRequest();
        request.setPhoneNumberSet(new String[] { formatPhoneNumber(target) });
        request.setSmsSdkAppId(properties.getSms().getSdkAppId());
        request.setSignName(properties.getSms().getSignName());
        request.setTemplateId(properties.getSms().getTemplateId());
        request.setTemplateParamSet(new String[] { code, String.valueOf(properties.getTtlSeconds() / 60) });
        try {
            smsClient.SendSms(request);
        } catch (TencentCloudSDKException e) {
            throw new IllegalStateException("Failed to send SMS verification code.", e);
        }
    }

    private String formatPhoneNumber(String target) {
        String normalized = target.trim().replaceAll("[\\s-]", "");
        if (normalized.startsWith("+")) {
            return normalized;
        }
        return "+86" + normalized;
    }
}
