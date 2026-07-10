package com.crm.customer.customer.mapper;

import com.crm.customer.customer.dto.response.CustomerDetailResponse;
import com.crm.customer.customer.dto.response.CustomerSearchResponse;
import com.crm.customer.customer.entity.Customer;
import com.crm.customer.customer.entity.Individual;
import com.crm.customer.customer.entity.Party;
import com.crm.customer.customer.entity.Role;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    /**
     * Used for rows freshly loaded from the DB (search/get), where the full
     * customer -> partyRole -> party -> individual/role graph is populated by
     * the query (see CustomerSpecifications' fetch joins).
     */
    public CustomerSearchResponse toSearchResponse(Customer customer) {
        Individual individual = customer.getPartyRole().getParty().getIndividual();
        Role role = customer.getPartyRole().getRole();
        return new CustomerSearchResponse(
                customer.getId(),
                individual.getFirstName(),
                individual.getMiddleName(),
                individual.getLastName(),
                role.getName(),
                individual.getNationalityId()
        );
    }

    public CustomerDetailResponse toDetailResponse(Customer customer) {
        Individual individual = customer.getPartyRole().getParty().getIndividual();
        Party party = customer.getPartyRole().getParty();
        Role role = customer.getPartyRole().getRole();
        return toDetailResponse(customer, individual, party, role);
    }

    /**
     * Used right after create/update, where the Party -> Individual inverse
     * association hasn't been refreshed from the DB yet (JPA does not sync the
     * mappedBy side of a OneToOne in memory) — callers pass the objects they
     * already hold instead of relying on entity graph navigation.
     */
    public CustomerDetailResponse toDetailResponse(Customer customer, Individual individual, Party party, Role role) {
        return new CustomerDetailResponse(
                customer.getId(),
                party.getId(),
                individual.getFirstName(),
                individual.getMiddleName(),
                individual.getLastName(),
                individual.getFatherName(),
                individual.getMotherName(),
                individual.getBirthDate(),
                individual.getGender().getApiValue(),
                individual.getNationalityId(),
                role.getName(),
                customer.getStatus().name()
        );
    }
}
