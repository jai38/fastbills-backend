package com.fastbills.api.hsn;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HsnItemRepository extends JpaRepository<HsnItem, UUID> {

    List<HsnItem> findAllByOrganisationIdAndIsActiveTrueOrderByCodeAsc(UUID organisationId);

    List<HsnItem> findAllByOrganisationIdOrderByCreatedAtDesc(UUID organisationId);

    Optional<HsnItem> findByIdAndOrganisationId(UUID id, UUID organisationId);

    Optional<HsnItem> findByOrganisationIdAndCode(UUID organisationId, String code);

    Optional<HsnItem> findByOrganisationIdAndIsDefaultTrueAndIsActiveTrue(UUID organisationId);

    boolean existsByOrganisationIdAndCode(UUID organisationId, String code);

    @Modifying
    @Query("UPDATE HsnItem h SET h.isDefault = false WHERE h.organisationId = :organisationId")
    void clearDefault(@Param("organisationId") UUID organisationId);
}
