package com.fastbills.api.receipt;

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
public interface ReceiptRepository extends JpaRepository<Receipt, UUID>, JpaSpecificationExecutor<Receipt> {

    List<Receipt> findByOrganisationIdAndInvoiceIdOrderByPaymentDateAscCreatedAtAsc(UUID organisationId, UUID invoiceId);

    Optional<Receipt> findByIdAndOrganisationId(UUID id, UUID organisationId);

    boolean existsByOrganisationIdAndInvoiceIdAndIsReversedFalse(UUID organisationId, UUID invoiceId);

    @Query("SELECT COALESCE(SUM(r.amount), 0.00) FROM Receipt r " +
            "WHERE r.organisationId = :organisationId AND r.invoiceId = :invoiceId AND r.isReversed = false")
    BigDecimal sumActiveAmountByInvoiceId(
            @Param("organisationId") UUID organisationId,
            @Param("invoiceId") UUID invoiceId
    );

    @Query("SELECT r.invoiceId, COALESCE(SUM(r.amount), 0.00) FROM Receipt r " +
            "WHERE r.organisationId = :organisationId AND r.invoiceId IN :invoiceIds AND r.isReversed = false " +
            "GROUP BY r.invoiceId")
    List<Object[]> sumActiveAmountsByInvoiceIds(
            @Param("organisationId") UUID organisationId,
            @Param("invoiceIds") Collection<UUID> invoiceIds
    );
}
