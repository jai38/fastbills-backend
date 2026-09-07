package com.fastbills.api.invoice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {

    @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.lines WHERE i.id = :id AND i.organisationId = :organisationId")
    Optional<Invoice> findByIdAndOrganisationIdWithLines(
            @Param("id") UUID id,
            @Param("organisationId") UUID organisationId
    );

    Optional<Invoice> findByIdAndOrganisationId(UUID id, UUID organisationId);

    @Query("SELECT DISTINCT i FROM Invoice i LEFT JOIN FETCH i.lines WHERE i.organisationId = :organisationId AND i.state = :state")
    List<Invoice> findAllByOrganisationIdAndStateWithLines(
            @Param("organisationId") UUID organisationId,
            @Param("state") InvoiceState state
    );

    boolean existsByCustomerIdAndState(UUID customerId, InvoiceState state);
}
