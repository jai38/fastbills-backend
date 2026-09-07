package com.fastbills.api.creditnote;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CreditNoteRepository extends JpaRepository<CreditNote, UUID>, JpaSpecificationExecutor<CreditNote> {

    Optional<CreditNote> findByIdAndOrganisationId(UUID id, UUID organisationId);

    @Query("SELECT cn FROM CreditNote cn LEFT JOIN FETCH cn.lines WHERE cn.id = :id AND cn.organisationId = :organisationId")
    Optional<CreditNote> findByIdAndOrganisationIdWithLines(
            @Param("id") UUID id,
            @Param("organisationId") UUID organisationId
    );

    List<CreditNote> findByOrganisationIdAndOriginalInvoiceIdOrderByCreatedAtDesc(UUID organisationId, UUID originalInvoiceId);

    List<CreditNote> findByOrganisationIdAndOriginalInvoiceIdAndState(UUID organisationId, UUID originalInvoiceId, CreditNoteState state);

    @Query("SELECT COALESCE(SUM(cn.grandTotal), 0.00) FROM CreditNote cn " +
            "WHERE cn.organisationId = :organisationId AND cn.originalInvoice.id = :invoiceId AND cn.state = 'ISSUED'")
    BigDecimal sumActiveAmountByInvoiceId(
            @Param("organisationId") UUID organisationId,
            @Param("invoiceId") UUID invoiceId
    );

    @Query("SELECT cn.originalInvoice.id, COALESCE(SUM(cn.grandTotal), 0.00) FROM CreditNote cn " +
            "WHERE cn.organisationId = :organisationId AND cn.originalInvoice.id IN :invoiceIds AND cn.state = 'ISSUED' " +
            "GROUP BY cn.originalInvoice.id")
    List<Object[]> sumActiveAmountsByInvoiceIds(
            @Param("organisationId") UUID organisationId,
            @Param("invoiceIds") Collection<UUID> invoiceIds
    );
}
