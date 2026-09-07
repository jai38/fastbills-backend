package com.fastbills.api.organisation;

import com.fastbills.api.auth.AuthenticatedUser;
import com.fastbills.api.common.tenant.TenantContext;
import com.fastbills.api.organisation.dto.UpdateOrganisationProfileRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organisation")
@Tag(name = "Organisation", description = "Organisation profile and branding management")
public class OrganisationController {

    private final OrganisationService organisationService;

    public OrganisationController(OrganisationService organisationService) {
        this.organisationService = organisationService;
    }

    @GetMapping("/profile")
    @Operation(summary = "Get current organisation profile", description = "Returns the organisation profile for the authenticated tenant")
    public ResponseEntity<Organisation> getCurrentOrganisationProfile() {
        return ResponseEntity.ok(organisationService.getProfile());
    }

    @PutMapping("/profile")
    @Operation(summary = "Update organisation profile", description = "Updates legal name, GSTIN, address, and bank details with validation")
    public ResponseEntity<Organisation> updateOrganisationProfile(
            @Valid @RequestBody UpdateOrganisationProfileRequest request
    ) {
        return ResponseEntity.ok(organisationService.updateProfile(request));
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload organisation logo", description = "Uploads a branding logo (JPEG/PNG/WebP, max 2MB)")
    public ResponseEntity<Map<String, String>> uploadLogo(@RequestParam("file") MultipartFile file) {
        String objectKey = organisationService.uploadLogo(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("objectKey", objectKey));
    }

    @GetMapping("/logo")
    @Operation(summary = "Get organisation logo", description = "Streams the active logo image for the authenticated organisation")
    public ResponseEntity<Resource> getLogo() {
        Resource logo = organisationService.getLogo();
        String contentType = "image/jpeg";
        try {
            if (logo.getFile() != null) {
                String probed = Files.probeContentType(logo.getFile().toPath());
                if (probed != null) {
                    contentType = probed;
                }
            }
        } catch (IOException ignored) {}

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                .body(logo);
    }

    @DeleteMapping("/logo")
    @Operation(summary = "Delete organisation logo", description = "Removes the branding logo from the organisation")
    public ResponseEntity<Void> deleteLogo() {
        organisationService.deleteLogo();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{orgId}")
    @Operation(summary = "Get organisation by ID (tenant scoped)", description = "Returns organisation by ID only if matching caller's tenant")
    public ResponseEntity<Organisation> getOrganisationById(
            @PathVariable UUID orgId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        UUID currentTenant = TenantContext.getRequiredTenant();
        if (!currentTenant.equals(orgId)) {
            throw new AccessDeniedException("Cross-tenant access prohibited");
        }
        return ResponseEntity.ok(organisationService.getProfile());
    }
}
