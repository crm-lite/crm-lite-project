package com.crm.customer.customer.repository;

import com.crm.customer.customer.entity.Customer;
import com.crm.customer.customer.entity.Individual;
import com.crm.customer.customer.entity.Party;
import com.crm.customer.customer.entity.PartyRole;
import com.crm.customer.customer.entity.Status;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Builds the FR-CUST-01 search predicate: ACTIVE customers only, firstName+lastName
 * form a single AND'd "name criterion", and that criterion is OR'd with nationalityId
 * and customerId (each only applied when provided).
 */
public final class CustomerSpecifications {

    private CustomerSpecifications() {
    }

    public static Specification<Customer> search(String firstName, String lastName, String nationalityId, Long customerId) {
        return (root, query, cb) -> {
            // NOTE: no query.distinct(true) here on purpose. Every join below
            // (partyRole/party/individual/role) is a to-one relationship, so this
            // query can never fan out into duplicate Customer rows in the first
            // place - DISTINCT would be a no-op at best. It was tried here but
            // removed: combined with ORDER BY on a joined table's column (see
            // CustomerController's Sort), Postgres rejects SELECT DISTINCT queries
            // whose ORDER BY expressions aren't part of the SELECT list.
            Join<Customer, PartyRole> partyRole = root.join("partyRole", JoinType.INNER);
            Join<PartyRole, Party> party = partyRole.join("party", JoinType.INNER);
            Join<Party, Individual> individual = party.join("individual", JoinType.INNER);
            partyRole.join("role", JoinType.INNER);

            Predicate activeOnly = cb.equal(root.get("status"), Status.ACTIVE);

            List<Predicate> criteria = new ArrayList<>();

            boolean hasFirstName = StringUtils.hasText(firstName);
            boolean hasLastName = StringUtils.hasText(lastName);
            if (hasFirstName || hasLastName) {
                Predicate namePredicate = cb.conjunction();
                if (hasFirstName) {
                    // Prefix match, not "contains" - firstName=li must not match "Ali"/"Velihan".
                    // Also matches middleName's prefix so firstName=Can finds "Ali Can Kaya".
                    String prefix = firstName.toLowerCase() + "%";
                    Predicate firstNameMatch = cb.like(cb.lower(individual.get("firstName")), prefix);
                    Predicate middleNameMatch = cb.like(cb.lower(individual.get("middleName")), prefix);
                    namePredicate = cb.and(namePredicate, cb.or(firstNameMatch, middleNameMatch));
                }
                if (hasLastName) {
                    namePredicate = cb.and(namePredicate,
                            cb.like(cb.lower(individual.get("lastName")), lastName.toLowerCase() + "%"));
                }
                criteria.add(namePredicate);
            }

            if (StringUtils.hasText(nationalityId)) {
                criteria.add(cb.equal(individual.get("nationalityId"), nationalityId));
            }

            if (customerId != null) {
                criteria.add(cb.equal(root.get("id"), customerId));
            }

            Predicate anyCriterion = cb.or(criteria.toArray(new Predicate[0]));
            return cb.and(activeOnly, anyCriterion);
        };
    }
}
