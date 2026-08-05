package com.crm.customer.account;

import java.util.Optional;

/**
 * account-service boundary for the KR-02 {@code accountNumber} search criterion
 * (ADR-013 §5: account_db has exactly one public reading point and it is this API —
 * customer-service never touches that database, never shares its entities and never
 * joins across it).
 *
 * <p>Implementations must keep three outcomes distinct:
 * <ul>
 *   <li>{@code Optional.empty()} — no visible account with that number: unknown, or
 *       the K-8 223 Customer Account, which is indistinguishable from unknown by
 *       design (ADR-013 §4.5) and must therefore never be searchable;
 *   <li>a summary — the account exists; its {@code accountStatus} says whether it is
 *       still Active;
 *   <li>{@link AccountServiceUnavailableException} — nothing is known. The search must
 *       fail closed rather than silently answer as if the number matched nothing.
 * </ul>
 *
 * <p>Reached directly via Eureka with the end user's token propagated (ADR-010), never
 * through the gateway, which is the browser edge (ADR-007).
 */
public interface AccountServiceClient {

    Optional<AccountSummary> fetchAccount(String accountNumber);
}
