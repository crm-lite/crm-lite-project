package com.crm.customer.account;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpAccountServiceClient implements AccountServiceClient {

    private final RestClient restClient;

    public HttpAccountServiceClient(@Qualifier("accountRestClient") RestClient accountRestClient) {
        this.restClient = accountRestClient;
    }

    @Override
    public Optional<AccountSummary> fetchAccount(String accountNumber) {
        try {
            return Optional.ofNullable(restClient.get()
                    .uri("/api/accounts/{accountNumber}", accountNumber)
                    .retrieve()
                    .body(AccountSummary.class));
        } catch (HttpClientErrorException.NotFound e) {
            // Unknown number, or the K-8 223 Customer Account (indistinguishable by
            // design — ADR-013 §4.5). Either way: nothing to match.
            return Optional.empty();
        } catch (RestClientException | IllegalStateException e) {
            // Connection failures, 5xx responses and "no instances available" from the
            // load balancer all mean the account domain is unavailable — never "the
            // account does not exist".
            throw new AccountServiceUnavailableException("account-service is unavailable", e);
        }
    }
}
