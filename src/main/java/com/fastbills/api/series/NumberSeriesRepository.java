package com.fastbills.api.series;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NumberSeriesRepository extends JpaRepository<NumberSeries, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ns FROM NumberSeries ns WHERE ns.organisationId = :organisationId AND ns.documentType = :documentType AND ns.financialYear = :financialYear")
    Optional<NumberSeries> findForUpdate(
            @Param("organisationId") UUID organisationId,
            @Param("documentType") DocumentType documentType,
            @Param("financialYear") Integer financialYear
    );

    Optional<NumberSeries> findByOrganisationIdAndDocumentTypeAndFinancialYear(
            UUID organisationId,
            DocumentType documentType,
            Integer financialYear
    );
}
