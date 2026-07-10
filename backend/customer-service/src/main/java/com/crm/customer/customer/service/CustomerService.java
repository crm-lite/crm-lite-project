package com.crm.customer.customer.service;

import com.crm.customer.customer.dto.request.CustomerCreateRequest;
import com.crm.customer.customer.dto.request.CustomerUpdateRequest;
import com.crm.customer.customer.dto.response.CustomerDetailResponse;
import com.crm.customer.customer.dto.response.CustomerSearchResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerService {

    Page<CustomerSearchResponse> search(String firstName, String lastName, String nationalityId, Long customerId,
                                         String accountNumber, String gsmNumber, String orderNumber, Pageable pageable);

    CustomerDetailResponse getById(Long customerId);

    CustomerDetailResponse create(CustomerCreateRequest request);

    CustomerDetailResponse update(Long customerId, CustomerUpdateRequest request);

    void delete(Long customerId);
}
