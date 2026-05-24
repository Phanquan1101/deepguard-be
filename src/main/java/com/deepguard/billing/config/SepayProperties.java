package com.deepguard.billing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.payment.sepay")
public class SepayProperties {

    private String webhookSecret;
    private String bankCode;
    private String bankAccountNo;
    private String bankAccountName;
    private String qrTemplate;
    private String qrBaseUrl;
    private String transferContentPrefix;
}
