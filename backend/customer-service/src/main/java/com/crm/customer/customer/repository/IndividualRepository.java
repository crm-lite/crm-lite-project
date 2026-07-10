package com.crm.customer.customer.repository;

import com.crm.customer.customer.entity.Individual;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IndividualRepository extends JpaRepository<Individual, Long> {
}
