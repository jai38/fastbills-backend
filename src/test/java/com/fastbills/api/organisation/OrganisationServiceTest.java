package com.fastbills.api.organisation;

import com.fastbills.api.common.tenant.TenantContext;
import com.fastbills.api.organisation.dto.UpdateOrganisationProfileRequest;
import com.fastbills.api.storage.StorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganisationServiceTest {

    @Mock
    private OrganisationRepository organisationRepository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private OrganisationService organisationService;

    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenant(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void testUpdateProfile_Success() {
        Organisation org = new Organisation("Old Legal Name");
        org.setId(tenantId);
        when(organisationRepository.findById(tenantId)).thenReturn(Optional.of(org));
        when(organisationRepository.save(any(Organisation.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateOrganisationProfileRequest request = new UpdateOrganisationProfileRequest(
                "New Legal Name Ltd",
                "New Trade",
                "27ABCDE1234F1Z5",
                "ABCDE1234F",
                "Line 1", "Line 2", "City", "27", "400001", "India",
                "9876543210", "info@org.com", "NLN",
                "Bank Account Name", "1234567890", "HDFC0001234", "HDFC", "Branch",
                "upi@hdfc", "Terms and conditions", 45
        );

        Organisation updated = organisationService.updateProfile(request);

        assertThat(updated.getLegalName()).isEqualTo("New Legal Name Ltd");
        assertThat(updated.getInvoicePrefix()).isEqualTo("NLN");
        assertThat(updated.getStateCode()).isEqualTo("27");
        assertThat(updated.getGstin()).isEqualTo("27ABCDE1234F1Z5");
        assertThat(updated.getDefaultCreditDays()).isEqualTo(45);
        verify(organisationRepository, times(1)).save(org);
    }

    @Test
    void testUpdateProfile_RejectsMismatchedGstinAndStateCode() {
        Organisation org = new Organisation("Org");
        org.setId(tenantId);
        when(organisationRepository.findById(tenantId)).thenReturn(Optional.of(org));

        UpdateOrganisationProfileRequest request = new UpdateOrganisationProfileRequest(
                "Legal Name Ltd",
                null,
                "27ABCDE1234F1Z5", // 27 Maharashtra
                null,
                null, null, null,
                "29", // 29 Karnataka mismatch
                null, null, null, null, "INV",
                null, null, null, null, null, null, null, 30
        );

        assertThatThrownBy(() -> organisationService.updateProfile(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("State code must match the first 2 digits of GSTIN (27)");

        verify(organisationRepository, never()).save(any(Organisation.class));
    }

    @Test
    void testUploadLogo_Success() {
        Organisation org = new Organisation("Org");
        org.setId(tenantId);
        when(organisationRepository.findById(tenantId)).thenReturn(Optional.of(org));
        when(storageService.store(anyString(), any(InputStream.class), anyString(), anyLong()))
                .thenReturn("logos/" + tenantId + "/mock.png");
        when(organisationRepository.save(any(Organisation.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "logo.png",
                "image/png",
                new byte[]{1, 2, 3, 4}
        );

        String key = organisationService.uploadLogo(file);

        assertThat(key).isNotBlank();
        assertThat(key).startsWith("logos/" + tenantId + "/");
        assertThat(org.getLogoObjectKey()).isEqualTo(key);
        verify(storageService, times(1)).store(anyString(), any(), eq("image/png"), eq(4L));
        verify(organisationRepository, times(1)).save(org);
    }

    @Test
    void testUploadLogo_RejectsInvalidContentType() {
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "script.sh",
                "application/x-sh",
                new byte[]{1, 2, 3}
        );

        assertThatThrownBy(() -> organisationService.uploadLogo(invalidFile))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid logo format");

        verify(storageService, never()).store(anyString(), any(), anyString(), anyLong());
    }

    @Test
    void testUploadLogo_RejectsOversizedFile() {
        byte[] largeBytes = new byte[2 * 1024 * 1024]; // 2MB > 1MB limit
        MockMultipartFile oversized = new MockMultipartFile(
                "file",
                "huge.png",
                "image/png",
                largeBytes
        );

        assertThatThrownBy(() -> organisationService.uploadLogo(oversized))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("exceeds maximum limit of 1MB");

        verify(storageService, never()).store(anyString(), any(), anyString(), anyLong());
    }
}
