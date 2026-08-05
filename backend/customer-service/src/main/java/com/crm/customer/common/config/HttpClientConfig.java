package com.crm.customer.common.config;

import com.crm.customer.account.AccountServiceProperties;
import com.crm.customer.lookup.LookupCatalogProperties;
import com.crm.customer.mernis.MernisProperties;
import com.crm.customer.order.OrderServiceProperties;
import com.crm.security.starter.BearerTokenPropagationInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Outbound HTTP clients for lookup-service (ADR-002), mernis-stub (KR-10) and — for
 * the KR-02 child-record search criteria — account-service (ADR-013 §5) and
 * order-service (ADR-016 §3.2). Service names in the base URLs are resolved through
 * Eureka by the load-balancer interceptor.
 *
 * Deliberately NOT exposed as a {@code @LoadBalanced RestClient.Builder} bean:
 * Spring Cloud's Eureka transport picks up any {@code RestClient.Builder} bean in the
 * context for its OWN registry calls, and a load-balanced builder makes Eureka try to
 * "load-balance" the literal host {@code localhost} — registration then fails with
 * "No instances available for localhost" (observed live). Building the concrete
 * RestClients here keeps the load balancing scoped to exactly these integrations.
 */
@Configuration
@EnableConfigurationProperties({LookupCatalogProperties.class, MernisProperties.class,
        AccountServiceProperties.class, OrderServiceProperties.class})
public class HttpClientConfig {

    @Bean
    public RestClient lookupRestClient(LoadBalancerClient loadBalancerClient, LookupCatalogProperties properties,
                                       BearerTokenPropagationInterceptor bearerTokenPropagation) {
        // ADR-010: lookup-service is an INTERNAL zero-trust resource server — the
        // end-user's Keycloak token is propagated so the subject and audience reach it.
        return RestClient.builder()
                .requestInterceptor(bearerTokenPropagation)
                .requestInterceptor(new LoadBalancerInterceptor(loadBalancerClient))
                .baseUrl(properties.baseUrl())
                .build();
    }

    @Bean
    public RestClient accountRestClient(LoadBalancerClient loadBalancerClient, AccountServiceProperties properties,
                                        BearerTokenPropagationInterceptor bearerTokenPropagation) {
        // ADR-010: account-service is an INTERNAL zero-trust resource server reached
        // directly via Eureka (never through the gateway, ADR-007). The end-user's
        // Keycloak token is propagated, so the subject and audience survive the hop and
        // the account read is authorized as that same operator — no client credentials
        // and no service account are introduced for this dependency.
        return RestClient.builder()
                .requestInterceptor(bearerTokenPropagation)
                .requestInterceptor(new LoadBalancerInterceptor(loadBalancerClient))
                .baseUrl(properties.baseUrl())
                .build();
    }

    @Bean
    public RestClient orderRestClient(LoadBalancerClient loadBalancerClient, OrderServiceProperties properties,
                                      BearerTokenPropagationInterceptor bearerTokenPropagation) {
        // ADR-010, same reasoning as accountRestClient above.
        return RestClient.builder()
                .requestInterceptor(bearerTokenPropagation)
                .requestInterceptor(new LoadBalancerInterceptor(loadBalancerClient))
                .baseUrl(properties.baseUrl())
                .build();
    }

    @Bean
    public RestClient mernisRestClient(LoadBalancerClient loadBalancerClient, MernisProperties properties) {
        // ADR-010: mernis-stub simulates an EXTERNAL KPS system. Deliberately NO
        // bearer propagation — a real KPS would never accept a CRM-realm JWT; user
        // attribution for verifications lives in CRM-side audit/log context instead.
        return RestClient.builder()
                .requestInterceptor(new LoadBalancerInterceptor(loadBalancerClient))
                .baseUrl(properties.baseUrl())
                .build();
    }
}
