package com.deepguard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DeepguardBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeepguardBeApplication.class, args);
    }

}
