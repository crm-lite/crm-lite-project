package com.crm.customer.customer.service.impl;

import com.crm.customer.customer.dto.request.CustomerCreateRequest;
import com.crm.customer.customer.dto.request.CustomerUpdateRequest;
import com.crm.customer.customer.dto.response.CustomerDetailResponse;
import com.crm.customer.customer.dto.response.CustomerSearchResponse;
import com.crm.customer.customer.entity.Customer;
import com.crm.customer.customer.entity.Individual;
import com.crm.customer.customer.entity.Party;
import com.crm.customer.customer.entity.PartyRole;
import com.crm.customer.customer.entity.Role;
import com.crm.customer.customer.entity.Status;
import com.crm.customer.customer.mapper.CustomerMapper;
import com.crm.customer.customer.repository.CustomerRepository;
import com.crm.customer.customer.repository.CustomerSpecifications;
import com.crm.customer.customer.repository.IndividualRepository;
import com.crm.customer.customer.repository.PartyRepository;
import com.crm.customer.customer.repository.PartyRoleRepository;
import com.crm.customer.customer.repository.RoleRepository;
import com.crm.customer.customer.rules.CustomerBusinessRules;
import com.crm.customer.customer.service.CustomerService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerServiceImpl implements CustomerService {

    // Seeded by V2__seed_customer_data.sql; looked up by code rather than hardcoding
    // the id so the mapping stays correct if the seed data is ever renumbered.
    private static final String CUSTOMER_ROLE_CODE = "CUSTOMER";

    private final CustomerRepository customerRepository;
    private final PartyRepository partyRepository;
    private final IndividualRepository individualRepository;
    private final PartyRoleRepository partyRoleRepository;
    private final RoleRepository roleRepository;
    private final CustomerBusinessRules businessRules;
    private final CustomerMapper customerMapper;

    @Override
    public Page<CustomerSearchResponse> search(String firstName, String lastName, String nationalityId, Long customerId,
                                                String accountNumber, String gsmNumber, String orderNumber,
                                                Pageable pageable) {
        businessRules.checkNoUnsupportedCrossServiceSearchCriterion(accountNumber, gsmNumber, orderNumber);
        businessRules.checkAtLeastOneSearchCriterionExists(firstName, lastName, nationalityId, customerId);

        Specification<Customer> spec = CustomerSpecifications.search(firstName, lastName, nationalityId, customerId);
        return customerRepository.findAll(spec, pageable).map(customerMapper::toSearchResponse);
    }

    @Override
    public CustomerDetailResponse getById(Long customerId) {
        Customer customer = businessRules.checkCustomerExistsAndActive(customerId);
        return customerMapper.toDetailResponse(customer);
    }

    @Override
    @Transactional
    public CustomerDetailResponse create(CustomerCreateRequest request) {
        businessRules.checkBirthDateIsNotFuture(request.getBirthDate());
        businessRules.checkCustomerIsAtLeast18(request.getBirthDate());
        businessRules.checkNationalityIdIsUniqueForCreate(request.getNationalityId());

        Instant now = Instant.now();

        Party party = new Party();
        party.setStatus(Status.ACTIVE);
        party.setCreatedAt(now);
        party = partyRepository.save(party);

        Individual individual = new Individual();
        individual.setParty(party);
        individual.setFirstName(request.getFirstName());
        individual.setMiddleName(request.getMiddleName());
        individual.setLastName(request.getLastName());
        individual.setFatherName(request.getFatherName());
        individual.setMotherName(request.getMotherName());
        individual.setBirthDate(request.getBirthDate());
        individual.setGender(request.getGender());
        individual.setNationalityId(request.getNationalityId());
        individual = individualRepository.save(individual);

        // FR-CUST-03 (full): once address-service and contact-service exist, customer
        // creation will be orchestrated across those services too (default address,
        // primary contact medium). This PR only creates the customer core records.
        Role customerRole = roleRepository.findByCode(CUSTOMER_ROLE_CODE)
                .orElseThrow(() -> new IllegalStateException(
                        "Seed data missing: role code '" + CUSTOMER_ROLE_CODE + "' not found"));

        PartyRole partyRole = new PartyRole();
        partyRole.setParty(party);
        partyRole.setRole(customerRole);
        partyRole.setStatus(Status.ACTIVE);
        partyRole = partyRoleRepository.save(partyRole);

        Customer customer = new Customer();
        customer.setPartyRole(partyRole);
        customer.setStatus(Status.ACTIVE);
        customer.setCreatedAt(now);
        customer = customerRepository.save(customer);

        return customerMapper.toDetailResponse(customer, individual, party, customerRole);
    }

    @Override
    @Transactional
    public CustomerDetailResponse update(Long customerId, CustomerUpdateRequest request) {
        Customer customer = businessRules.checkCustomerExistsAndActive(customerId);

        businessRules.checkBirthDateIsNotFuture(request.getBirthDate());
        businessRules.checkCustomerIsAtLeast18(request.getBirthDate());
        businessRules.checkNationalityIdIsUniqueForUpdate(request.getNationalityId(), customerId);

        PartyRole partyRole = customer.getPartyRole();
        Party party = partyRole.getParty();
        Individual individual = party.getIndividual();

        individual.setFirstName(request.getFirstName());
        individual.setMiddleName(request.getMiddleName());
        individual.setLastName(request.getLastName());
        individual.setFatherName(request.getFatherName());
        individual.setMotherName(request.getMotherName());
        individual.setBirthDate(request.getBirthDate());
        individual.setGender(request.getGender());
        individual.setNationalityId(request.getNationalityId());
        individualRepository.save(individual);

        Instant now = Instant.now();
        party.setUpdatedAt(now);
        customer.setUpdatedAt(now);

        return customerMapper.toDetailResponse(customer, individual, party, partyRole.getRole());
    }

    @Override
    @Transactional
    public void delete(Long customerId) {
        Customer customer = businessRules.checkCustomerExistsAndActive(customerId);
        businessRules.checkCustomerHasNoActiveProducts(customerId);

        Instant now = Instant.now();

        PartyRole partyRole = customer.getPartyRole();
        Party party = partyRole.getParty();

        customer.setStatus(Status.PASSIVE);
        customer.setUpdatedAt(now);

        partyRole.setStatus(Status.PASSIVE);

        party.setStatus(Status.PASSIVE);
        party.setUpdatedAt(now);
    }
}
