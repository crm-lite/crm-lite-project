package com.crm.customer.account;

/**
 * account-service could not be reached (or answered with an unexpected error) while
 * resolving an {@code accountNumber} search criterion.
 *
 * <p>The search fails closed with 503 {@code MSG-SERVICE-UNAVAILABLE}: treating the
 * outage as "the number matched nothing" would answer MSG-CUST-NOT-FOUND for a
 * customer that exists, and treating it as "match everything" would leak the whole
 * list. Neither is an acceptable substitute for saying the query could not be run.
 */
public class AccountServiceUnavailableException extends RuntimeException {

    public AccountServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
