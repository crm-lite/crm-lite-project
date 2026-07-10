package com.crm.customer.customer.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.crm.customer.customer.entity.Customer;
import com.crm.customer.customer.entity.Gender;
import com.crm.customer.customer.entity.Individual;
import com.crm.customer.customer.entity.Party;
import com.crm.customer.customer.entity.PartyRole;
import com.crm.customer.customer.entity.Role;
import com.crm.customer.customer.entity.Status;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * Runs against the real local Postgres (see src/test/resources/application.yml) instead of an
 * embedded database - this project has no H2/Testcontainers dependency, and Postgres-specific
 * LIKE/lower() prefix-matching behavior is exactly what needs verifying here. Requires the local
 * dev Postgres to be up (docker-compose/Podman); @DataJpaTest rolls back each test's changes
 * automatically, so seed data is left untouched.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CustomerSpecificationsTest {

    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PartyRepository partyRepository;
    @Autowired
    private IndividualRepository individualRepository;
    @Autowired
    private PartyRoleRepository partyRoleRepository;
    @Autowired
    private TestEntityManager entityManager;

    private void createCustomer(String firstName, String middleName, String lastName, String nationalityId) {
        Role role = roleRepository.findByCode("CUSTOMER").orElseThrow();

        Party party = new Party();
        party.setStatus(Status.ACTIVE);
        party.setCreatedAt(Instant.now());
        party = partyRepository.save(party);

        Individual individual = new Individual();
        individual.setParty(party);
        individual.setFirstName(firstName);
        individual.setMiddleName(middleName);
        individual.setLastName(lastName);
        individual.setBirthDate(LocalDate.of(1990, 1, 1));
        individual.setGender(Gender.MALE);
        individual.setNationalityId(nationalityId);
        individualRepository.save(individual);

        PartyRole partyRole = new PartyRole();
        partyRole.setParty(party);
        partyRole.setRole(role);
        partyRole.setStatus(Status.ACTIVE);
        partyRole = partyRoleRepository.save(partyRole);

        Customer customer = new Customer();
        customer.setPartyRole(partyRole);
        customer.setStatus(Status.ACTIVE);
        customer.setCreatedAt(Instant.now());
        customerRepository.save(customer);

        // Party.individual is the inverse (mappedBy) side of a OneToOne: Hibernate never
        // populates it in memory just because Individual.party was set above. Flush + clear
        // so the specification's search query re-hydrates everything fresh from the DB
        // instead of returning the still-half-populated managed instances from this session.
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void firstNameSearch_isPrefixNotContains() {
        createCustomer("Aylin", null, "Test1", "90000000001");
        createCustomer("Ayse", null, "Test2", "90000000002");

        List<Customer> prefixMatch = customerRepository.findAll(
                CustomerSpecifications.search("Ayl", null, null, null));
        assertThat(prefixMatch).extracting(c -> c.getPartyRole().getParty().getIndividual().getFirstName())
                .containsExactly("Aylin");

        List<Customer> substringOnly = customerRepository.findAll(
                CustomerSpecifications.search("yl", null, null, null));
        assertThat(substringOnly).isEmpty();
    }

    @Test
    void firstNameSearch_alsoMatchesMiddleNamePrefix() {
        createCustomer("Deniz", "Kaan", "Test3", "90000000003");

        List<Customer> byMiddleName = customerRepository.findAll(
                CustomerSpecifications.search("Kaa", null, null, null));
        assertThat(byMiddleName).extracting(c -> c.getPartyRole().getParty().getIndividual().getMiddleName())
                .containsExactly("Kaan");
    }

    @Test
    void firstNameAndLastName_areAndedTogether() {
        createCustomer("Ece", null, "Yalcin", "90000000004");
        createCustomer("Ece", null, "Demir", "90000000005");

        List<Customer> both = customerRepository.findAll(
                CustomerSpecifications.search("Ece", "Yal", null, null));
        assertThat(both).extracting(c -> c.getPartyRole().getParty().getIndividual().getLastName())
                .containsExactly("Yalcin");
    }
}
