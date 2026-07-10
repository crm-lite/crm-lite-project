package com.crm.customer.customer.rules;

import com.crm.customer.common.exception.BusinessException;
import com.crm.customer.common.exception.MessageKeys;
import com.crm.customer.customer.entity.Customer;
import com.crm.customer.customer.entity.Status;
import com.crm.customer.customer.repository.CustomerRepository;
import java.time.LocalDate;
import java.time.Period;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class CustomerBusinessRules {

    private static final int MIN_AGE = 18;

    private final CustomerRepository customerRepository;

    public void checkAtLeastOneSearchCriterionExists(String firstName, String lastName, String nationalityId, Long customerId) {
        boolean anyProvided = StringUtils.hasText(firstName)
                || StringUtils.hasText(lastName)
                || StringUtils.hasText(nationalityId)
                || customerId != null;
        if (!anyProvided) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, MessageKeys.SEARCH_CRITERIA_REQUIRED,
                    "At least one search criterion must be provided");
        }
    }

    // TODO: accountNumber/gsmNumber/orderNumber will be implemented once account-service,
    // contact-service, and order-service exist and can be integrated with for these lookups.
    public void checkNoUnsupportedCrossServiceSearchCriterion(String accountNumber, String gsmNumber, String orderNumber) {
        if (StringUtils.hasText(accountNumber) || StringUtils.hasText(gsmNumber) || StringUtils.hasText(orderNumber)) {
            throw new BusinessException(HttpStatus.NOT_IMPLEMENTED, MessageKeys.FEATURE_NOT_IMPLEMENTED,
                    "Search by accountNumber/gsmNumber/orderNumber is not implemented yet");
        }
    }

    public Customer checkCustomerExistsAndActive(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, MessageKeys.CUST_NOT_FOUND,
                        "Customer not found: " + customerId));
        if (customer.getStatus() != Status.ACTIVE) {
            throw new BusinessException(HttpStatus.NOT_FOUND, MessageKeys.CUST_NOT_FOUND,
                    "Customer not found: " + customerId);
        }
        return customer;
    }

    public void checkNationalityIdIsUniqueForCreate(String nationalityId) {
        if (customerRepository.existsActiveByNationalityId(nationalityId)) {
            throw new BusinessException(HttpStatus.CONFLICT, MessageKeys.CUST_DUP_NATID,
                    "nationalityId is already used by an active customer: " + nationalityId);
        }
    }

    public void checkNationalityIdIsUniqueForUpdate(String nationalityId, Long customerId) {
        if (customerRepository.existsActiveByNationalityIdExcludingCustomer(nationalityId, customerId)) {
            throw new BusinessException(HttpStatus.CONFLICT, MessageKeys.CUST_DUP_NATID,
                    "nationalityId is already used by another active customer: " + nationalityId);
        }
    }

    public void checkBirthDateIsNotFuture(LocalDate birthDate) {
        if (birthDate.isAfter(LocalDate.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, MessageKeys.VAL_BIRTHDATE,
                    "birthDate cannot be in the future");
        }
    }

    public void checkCustomerIsAtLeast18(LocalDate birthDate) {
        if (Period.between(birthDate, LocalDate.now()).getYears() < MIN_AGE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, MessageKeys.VAL_AGE_MIN,
                    "Customer must be at least " + MIN_AGE + " years old");
        }
    }

    // TODO: once product-service/account-service exist, call them here to verify the
    // customer has no active products/accounts before allowing a soft delete. For now
    // this always passes (assumes no active products), per current PR scope.
    public void checkCustomerHasNoActiveProducts(Long customerId) {
        // no-op
    }
}
