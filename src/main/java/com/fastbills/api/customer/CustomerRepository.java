package com.fastbills.api.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID>, JpaSpecificationExecutor<Customer> {

    boolean existsByOrganisationIdAndLegalNameIgnoreCase(UUID organisationId, String legalName);

    boolean existsByOrganisationIdAndLegalNameIgnoreCaseAndIdNot(UUID organisationId, String legalName, UUID id);

    Optional<Customer> findByIdAndOrganisationId(UUID id, UUID organisationId);
}
