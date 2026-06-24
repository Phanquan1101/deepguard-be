package com.deepguard.scan.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.hive")
public class HiveProperties {

    private String apiKey;
    private String baseUrl;
    private String modelName = "ai-generated-and-deepfake-content-detection";

    public String getFullApiUrl() {
        return baseUrl + "/hive/" + modelName;
    }
}