package com.fastbills.api.organisation;

import com.fastbills.api.common.tenant.TenantContext;
import com.fastbills.api.organisation.dto.UpdateOrganisationProfileRequest;
import com.fastbills.api.storage.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
public class OrganisationService {

    private static final long MAX_LOGO_SIZE_BYTES = 1 * 1024 * 1024; // 1 MB
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final OrganisationRepository organisationRepository;
    private final StorageService storageService;

    public OrganisationService(OrganisationRepository organisationRepository, StorageService storageService) {
        this.organisationRepository = organisationRepository;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public Organisation getProfile() {
        UUID orgId = TenantContext.getRequiredTenant();
        return organisationRepository.findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organisation not found"));
    }

    @Transactional
    public Organisation updateProfile(UpdateOrganisationProfileRequest request) {
        UUID orgId = TenantContext.getRequiredTenant();
        Organisation org = organisationRepository.findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organisation not found"));

        String gstin = cleanString(request.gstin());
        String stateCode = cleanString(request.stateCode());

        // Validate GSTIN to state code consistency
        if (gstin != null) {
            if (gstin.length() != 15) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "GSTIN must be exactly 15 characters long");
            }
            String gstinState = gstin.substring(0, 2);
            if (stateCode == null || !gstinState.equals(stateCode)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "State code must match the first 2 digits of GSTIN (" + gstinState + ")"
                );
            }
        }

        org.setLegalName(request.legalName().trim());
        org.setTradeName(cleanString(request.tradeName()));
        org.setGstin(gstin);
        org.setPan(cleanString(request.pan()));
        org.setAddressLine1(cleanString(request.addressLine1()));
        org.setAddressLine2(cleanString(request.addressLine2()));
        org.setCity(cleanString(request.city()));
        org.setStateCode(stateCode);
        org.setPincode(cleanString(request.pincode()));
        if (request.country() != null && !request.country().isBlank()) {
            org.setCountry(request.country().trim());
        }
        org.setPhone(cleanString(request.phone()));
        org.setEmail(cleanString(request.email()));
        org.setInvoicePrefix(request.invoicePrefix().trim().toUpperCase());
        org.setBankAccountName(cleanString(request.bankAccountName()));
        org.setBankAccountNumber(cleanString(request.bankAccountNumber()));
        org.setBankIfsc(cleanString(request.bankIfsc()));
        org.setBankName(cleanString(request.bankName()));
        org.setBankBranch(cleanString(request.bankBranch()));
        org.setUpiId(cleanString(request.upiId()));
        org.setDefaultTermsText(cleanString(request.defaultTermsText()));
        if (request.defaultCreditDays() != null && request.defaultCreditDays() >= 0) {
            org.setDefaultCreditDays(request.defaultCreditDays());
        }

        return organisationRepository.save(org);
    }

    @Transactional
    public String uploadLogo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Logo file cannot be empty");
        }

        if (file.getSize() > MAX_LOGO_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Logo file size exceeds maximum limit of 1MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid logo format. Only JPEG, PNG, and WebP are allowed"
            );
        }

        UUID orgId = TenantContext.getRequiredTenant();
        Organisation org = organisationRepository.findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organisation not found"));

        String extension = getFileExtension(file.getOriginalFilename(), contentType);
        String objectKey = "logos/" + orgId + "/" + UUID.randomUUID() + extension;

        try {
            storageService.store(objectKey, file.getInputStream(), contentType, file.getSize());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload logo", e);
        }

        // Delete previous logo if any
        if (org.getLogoObjectKey() != null) {
            try {
                storageService.delete(org.getLogoObjectKey());
            } catch (Exception ignored) {}
        }

        org.setLogoObjectKey(objectKey);
        organisationRepository.save(org);
        return objectKey;
    }

    @Transactional(readOnly = true)
    public Resource getLogo() {
        UUID orgId = TenantContext.getRequiredTenant();
        Organisation org = organisationRepository.findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organisation not found"));

        if (org.getLogoObjectKey() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Organisation has no logo configured");
        }

        return storageService.load(org.getLogoObjectKey());
    }

    @Transactional
    public void deleteLogo() {
        UUID orgId = TenantContext.getRequiredTenant();
        Organisation org = organisationRepository.findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organisation not found"));

        if (org.getLogoObjectKey() != null) {
            storageService.delete(org.getLogoObjectKey());
            org.setLogoObjectKey(null);
            organisationRepository.save(org);
        }
    }

    private String cleanString(String val) {
        if (val == null) return null;
        String trimmed = val.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String getFileExtension(String filename, String contentType) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf(".")).toLowerCase();
        }
        return switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
