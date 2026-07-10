package com.crm.customer.customer.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.crm.customer.common.exception.BusinessException;
import com.crm.customer.common.exception.MessageKeys;
import com.crm.customer.customer.entity.Customer;
import com.crm.customer.customer.entity.Status;
import com.crm.customer.customer.repository.CustomerRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class CustomerBusinessRulesTest {

    private CustomerRepository customerRepository;
    private CustomerBusinessRules rules;

    @BeforeEach
    void setUp() {
        customerRepository = mock(CustomerRepository.class);
        rules = new CustomerBusinessRules(customerRepository);
    }

    @Test
    void checkAtLeastOneSearchCriterionExists_throwsWhenNoneProvided() {
        assertThatThrownBy(() -> rules.checkAtLeastOneSearchCriterionExists(null, "  ", null, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(be.getMessageKey()).isEqualTo(MessageKeys.SEARCH_CRITERIA_REQUIRED);
                });
    }

    @Test
    void checkAtLeastOneSearchCriterionExists_passesWhenCustomerIdProvided() {
        rules.checkAtLeastOneSearchCriterionExists(null, null, null, 42L);
    }

    @Test
    void checkNoUnsupportedCrossServiceSearchCriterion_throwsWhenGsmNumberProvided() {
        assertThatThrownBy(() -> rules.checkNoUnsupportedCrossServiceSearchCriterion(null, "05321112233", null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
                    assertThat(be.getMessageKey()).isEqualTo(MessageKeys.FEATURE_NOT_IMPLEMENTED);
                });
    }

    @Test
    void checkNoUnsupportedCrossServiceSearchCriterion_passesWhenNoneProvided() {
        rules.checkNoUnsupportedCrossServiceSearchCriterion(null, null, null);
    }

    @Test
    void checkCustomerExistsAndActive_throwsWhenNotFound() {
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rules.checkCustomerExistsAndActive(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getMessageKey()).isEqualTo(MessageKeys.CUST_NOT_FOUND));
    }

    @Test
    void checkCustomerExistsAndActive_throwsWhenPassive() {
        Customer passive = new Customer();
        passive.setStatus(Status.PASSIVE);
        when(customerRepository.findById(2L)).thenReturn(Optional.of(passive));

        assertThatThrownBy(() -> rules.checkCustomerExistsAndActive(2L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getMessageKey()).isEqualTo(MessageKeys.CUST_NOT_FOUND));
    }

    @Test
    void checkCustomerExistsAndActive_returnsCustomerWhenActive() {
        Customer active = new Customer();
        active.setStatus(Status.ACTIVE);
        when(customerRepository.findById(3L)).thenReturn(Optional.of(active));

        assertThat(rules.checkCustomerExistsAndActive(3L)).isSameAs(active);
    }

    @Test
    void checkNationalityIdIsUniqueForCreate_throwsWhenAlreadyActive() {
        when(customerRepository.existsActiveByNationalityId("10000000001")).thenReturn(true);

        assertThatThrownBy(() -> rules.checkNationalityIdIsUniqueForCreate("10000000001"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getMessageKey()).isEqualTo(MessageKeys.CUST_DUP_NATID);
                });
    }

    @Test
    void checkNationalityIdIsUniqueForCreate_passesWhenFree() {
        when(customerRepository.existsActiveByNationalityId("10000000009")).thenReturn(false);
        rules.checkNationalityIdIsUniqueForCreate("10000000009");
    }

    @Test
    void checkNationalityIdIsUniqueForUpdate_throwsWhenUsedByAnotherActiveCustomer() {
        when(customerRepository.existsActiveByNationalityIdExcludingCustomer("10000000001", 5L)).thenReturn(true);

        assertThatThrownBy(() -> rules.checkNationalityIdIsUniqueForUpdate("10000000001", 5L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getMessageKey()).isEqualTo(MessageKeys.CUST_DUP_NATID));
    }

    @Test
    void checkBirthDateIsNotFuture_throwsWhenInFuture() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        assertThatThrownBy(() -> rules.checkBirthDateIsNotFuture(tomorrow))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getMessageKey()).isEqualTo(MessageKeys.VAL_BIRTHDATE));
    }

    @Test
    void checkBirthDateIsNotFuture_passesForPastDate() {
        rules.checkBirthDateIsNotFuture(LocalDate.now().minusYears(20));
    }

    @Test
    void checkCustomerIsAtLeast18_throwsWhenUnder18() {
        LocalDate seventeenYearsAgo = LocalDate.now().minusYears(17);
        assertThatThrownBy(() -> rules.checkCustomerIsAtLeast18(seventeenYearsAgo))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getMessageKey()).isEqualTo(MessageKeys.VAL_AGE_MIN));
    }

    @Test
    void checkCustomerIsAtLeast18_passesWhenExactly18() {
        rules.checkCustomerIsAtLeast18(LocalDate.now().minusYears(18));
    }
}
