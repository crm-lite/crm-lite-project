package com.crm.customer.customer.repository;

import com.crm.customer.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

    @Query("""
            SELECT COUNT(c) > 0 FROM Customer c
            WHERE c.status = com.crm.customer.customer.entity.Status.ACTIVE
            AND c.partyRole.party.individual.nationalityId = :nationalityId
            """)
    boolean existsActiveByNationalityId(@Param("nationalityId") String nationalityId);

    @Query("""
            SELECT COUNT(c) > 0 FROM Customer c
            WHERE c.status = com.crm.customer.customer.entity.Status.ACTIVE
            AND c.partyRole.party.individual.nationalityId = :nationalityId
            AND c.id <> :customerId
            """)
    boolean existsActiveByNationalityIdExcludingCustomer(@Param("nationalityId") String nationalityId,
                                                          @Param("customerId") Long customerId);
}
