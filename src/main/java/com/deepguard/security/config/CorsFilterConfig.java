package com.deepguard.security.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
public class CorsFilterConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration() {
        CorsFilter corsFilter = new CorsFilter(corsConfigurationSource);
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(corsFilter);
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
