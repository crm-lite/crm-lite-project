package com.crm.customer.customer.controller;

import com.crm.customer.customer.dto.request.CustomerCreateRequest;
import com.crm.customer.customer.dto.request.CustomerUpdateRequest;
import com.crm.customer.customer.dto.response.CustomerDetailResponse;
import com.crm.customer.customer.dto.response.CustomerSearchResponse;
import com.crm.customer.customer.service.CustomerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Validated
public class CustomerController {

    // AC-CUST-01-07: no dedicated message key exists for this rule in the given
    // catalog (its validation table row has no Format Ref / Mesaj entry), so a
    // plain description is used here, same precedent as CustomerCreateRequest's
    // gender field.
    private static final String NUMERIC_REGEX = "^[0-9]+$";
    private static final String NUMERIC_ONLY_MESSAGE = "must contain digits only";

    private final CustomerService customerService;

    // Canonical search endpoint (preferred). GET /api/customers/search below is kept as a
    // backward-compatible alias for existing callers/scripts.
    @GetMapping
    public ResponseEntity<Page<CustomerSearchResponse>> search(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) @Pattern(regexp = NUMERIC_REGEX, message = NUMERIC_ONLY_MESSAGE) String nationalityId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) @Pattern(regexp = NUMERIC_REGEX, message = NUMERIC_ONLY_MESSAGE) String accountNumber,
            @RequestParam(required = false) @Pattern(regexp = NUMERIC_REGEX, message = NUMERIC_ONLY_MESSAGE) String gsmNumber,
            @RequestParam(required = false) @Pattern(regexp = NUMERIC_REGEX, message = NUMERIC_ONLY_MESSAGE) String orderNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.asc("partyRole.party.individual.firstName"),
                        Sort.Order.asc("partyRole.party.individual.lastName")));

        Page<CustomerSearchResponse> result = customerService.search(
                firstName, lastName, nationalityId, customerId, accountNumber, gsmNumber, orderNumber, pageable);
        return ResponseEntity.ok(result);
    }

    // Legacy alias: kept for backward compatibility with existing scripts/bookmarks.
    // Prefer GET /api/customers (see search() above) for new callers.
    @GetMapping("/search")
    public ResponseEntity<Page<CustomerSearchResponse>> searchLegacyAlias(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) @Pattern(regexp = NUMERIC_REGEX, message = NUMERIC_ONLY_MESSAGE) String nationalityId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) @Pattern(regexp = NUMERIC_REGEX, message = NUMERIC_ONLY_MESSAGE) String accountNumber,
            @RequestParam(required = false) @Pattern(regexp = NUMERIC_REGEX, message = NUMERIC_ONLY_MESSAGE) String gsmNumber,
            @RequestParam(required = false) @Pattern(regexp = NUMERIC_REGEX, message = NUMERIC_ONLY_MESSAGE) String orderNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return search(firstName, lastName, nationalityId, customerId, accountNumber, gsmNumber, orderNumber, page, size);
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerDetailResponse> getById(@PathVariable Long customerId) {
        return ResponseEntity.ok(customerService.getById(customerId));
    }

    @PostMapping
    public ResponseEntity<CustomerDetailResponse> create(@Valid @RequestBody CustomerCreateRequest request) {
        CustomerDetailResponse response = customerService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{customerId}")
    public ResponseEntity<CustomerDetailResponse> update(@PathVariable Long customerId,
                                                          @Valid @RequestBody CustomerUpdateRequest request) {
        return ResponseEntity.ok(customerService.update(customerId, request));
    }

    @DeleteMapping("/{customerId}")
    public ResponseEntity<Void> delete(@PathVariable Long customerId) {
        customerService.delete(customerId);
        return ResponseEntity.noContent().build();
    }
}
