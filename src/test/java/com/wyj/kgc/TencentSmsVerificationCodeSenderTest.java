package com.wyj.kgc;

import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.wyj.kgc.config.VerificationCodeProperties;
import com.wyj.kgc.service.TencentSmsVerificationCodeSender;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TencentSmsVerificationCodeSenderTest {

    @Test
    void sendsSixDigitCodeBySms() throws Exception {
        SmsClient smsClient = mock(SmsClient.class);
        VerificationCodeProperties properties = new VerificationCodeProperties();
        properties.getSms().setSdkAppId("1400000000");
        properties.getSms().setSignName("KGC");
        properties.getSms().setTemplateId("123456");
        TencentSmsVerificationCodeSender sender = new TencentSmsVerificationCodeSender(smsClient, properties);

        sender.send("13800138000", "123456");

        ArgumentCaptor<SendSmsRequest> requestCaptor = ArgumentCaptor.forClass(SendSmsRequest.class);
        verify(smsClient).SendSms(requestCaptor.capture());
        SendSmsRequest request = requestCaptor.getValue();
        assertThat(request.getPhoneNumberSet()).containsExactly("+8613800138000");
        assertThat(request.getSmsSdkAppId()).isEqualTo("1400000000");
        assertThat(request.getSignName()).isEqualTo("KGC");
        assertThat(request.getTemplateId()).isEqualTo("123456");
        assertThat(request.getTemplateParamSet()).containsExactly("123456", "5");
    }
}
