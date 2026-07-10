package com.crm.customer.customer.dto.response;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CustomerDetailResponse {

    private final Long customerId;
    private final Long partyId;
    private final String firstName;
    private final String middleName;
    private final String lastName;
    private final String fatherName;
    private final String motherName;
    private final LocalDate birthDate;
    private final String gender;
    private final String nationalityId;
    private final String role;
    private final String status;
}
