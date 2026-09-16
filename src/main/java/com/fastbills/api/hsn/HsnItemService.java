package com.fastbills.api.hsn;

import com.fastbills.api.common.tenant.TenantContext;
import com.fastbills.api.hsn.dto.CreateHsnItemRequest;
import com.fastbills.api.hsn.dto.HsnItemResponse;
import com.fastbills.api.hsn.dto.HsnLookupResponse;
import com.fastbills.api.hsn.dto.UpdateHsnItemRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class HsnItemService {

    private final HsnItemRepository repository;
    private final HsnValidationService validationService;

    public HsnItemService(HsnItemRepository repository, HsnValidationService validationService) {
        this.repository = repository;
        this.validationService = validationService;
    }

    @Transactional(readOnly = true)
    public List<HsnItemResponse> listActive(UUID organisationId) {
        return repository.findAllByOrganisationIdAndIsActiveTrueOrderByCodeAsc(organisationId)
                .stream()
                .map(HsnItemResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<HsnItemResponse> listAll(UUID organisationId) {
        return repository.findAllByOrganisationIdOrderByCreatedAtDesc(organisationId)
                .stream()
                .map(HsnItemResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public HsnItemResponse getById(UUID organisationId, UUID id) {
        return repository.findByIdAndOrganisationId(id, organisationId)
                .map(HsnItemResponse::fromEntity)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "HSN item not found"));
    }

    @Transactional(readOnly = true)
    public HsnItemResponse getDefault(UUID organisationId) {
        return repository.findByOrganisationIdAndIsDefaultTrueAndIsActiveTrue(organisationId)
                .map(HsnItemResponse::fromEntity)
                .orElse(null);
    }

    @Transactional
    public HsnItemResponse create(UUID organisationId, CreateHsnItemRequest request) {
        String cleanCode = request.code().trim().replaceAll("[^0-9]", "");
        if (repository.existsByOrganisationIdAndCode(organisationId, cleanCode)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "HSN/SAC code " + cleanCode + " already exists in your directory");
        }

        // Auto-validate and auto-populate name/desc if not provided
        HsnLookupResponse lookup = validationService.lookup(cleanCode);
        String name = (request.name() != null && !request.name().trim().isEmpty())
                ? request.name().trim()
                : lookup.name();
        String description = (request.description() != null && !request.description().trim().isEmpty())
                ? request.description().trim()
                : lookup.description();
        boolean isService = request.isService() != null ? request.isService() : lookup.isService();

        boolean isDefault = Boolean.TRUE.equals(request.isDefault());
        if (isDefault) {
            repository.clearDefault(organisationId);
        }

        HsnItem item = new HsnItem(
                organisationId,
                cleanCode,
                name,
                description,
                request.taxRatePercent(),
                isService,
                isDefault
        );

        HsnItem saved = repository.save(item);
        return HsnItemResponse.fromEntity(saved);
    }

    @Transactional
    public HsnItemResponse update(UUID organisationId, UUID id, UpdateHsnItemRequest request) {
        HsnItem item = repository.findByIdAndOrganisationId(id, organisationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "HSN item not found"));

        boolean isDefault = Boolean.TRUE.equals(request.isDefault());
        if (isDefault && !Boolean.TRUE.equals(item.getIsDefault())) {
            repository.clearDefault(organisationId);
        }

        item.setName(request.name().trim());
        item.setDescription(request.description() != null ? request.description().trim() : null);
        item.setTaxRatePercent(request.taxRatePercent());
        if (request.isService() != null) item.setIsService(request.isService());
        if (request.isDefault() != null) item.setIsDefault(request.isDefault());
        if (request.isActive() != null) item.setIsActive(request.isActive());

        HsnItem saved = repository.save(item);
        return HsnItemResponse.fromEntity(saved);
    }

    @Transactional
    public HsnItemResponse setDefault(UUID organisationId, UUID id) {
        HsnItem item = repository.findByIdAndOrganisationId(id, organisationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "HSN item not found"));

        repository.clearDefault(organisationId);
        item.setIsDefault(true);
        item.setIsActive(true);
        HsnItem saved = repository.save(item);
        return HsnItemResponse.fromEntity(saved);
    }

    @Transactional
    public void delete(UUID organisationId, UUID id) {
        HsnItem item = repository.findByIdAndOrganisationId(id, organisationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "HSN item not found"));
        repository.delete(item);
    }

    public HsnLookupResponse validate(String code) {
        return validationService.lookup(code);
    }
}
