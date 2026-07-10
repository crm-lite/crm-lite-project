package com.crm.customer.customer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CustomerSearchResponse {

    private final Long customerId;
    private final String firstName;
    private final String middleName;
    private final String lastName;
    private final String role;
    private final String nationalityId;
}
