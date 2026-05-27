package com.deepguard.scan.config;

import lombok.Getter;
import lombok.Setter;

// @Configuration and @ConfigurationProperties removed; deprecated
@Getter
@Setter
public class AIProperties {

    private String pythonServerUrl;

    public String getPredictUrl() {
        return pythonServerUrl + "/predict";
    }
}
