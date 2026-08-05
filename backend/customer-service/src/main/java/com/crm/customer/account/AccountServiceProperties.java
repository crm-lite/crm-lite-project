package com.crm.customer.account;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param baseUrl account-service base URL. The default is the Eureka service name
 *                resolved by the load-balanced RestClient; override with a concrete
 *                http://host:port for environments without discovery.
 */
@ConfigurationProperties(prefix = "crm.account")
public record AccountServiceProperties(String baseUrl) {

    public AccountServiceProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://account-service";
        }
    }
}
