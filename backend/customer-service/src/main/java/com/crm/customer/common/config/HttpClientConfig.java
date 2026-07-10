package com.crm.customer.common.config;

import com.crm.customer.lookup.LookupCatalogProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Shared load-balanced RestClient builder for outbound calls (lookup-service,
 * mernis-stub). Service names in base URLs (http://lookup-service, ...) are resolved
 * through Eureka by Spring Cloud LoadBalancer.
 */
@Configuration
@EnableConfigurationProperties(LookupCatalogProperties.class)
public class HttpClientConfig {

    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }
}
