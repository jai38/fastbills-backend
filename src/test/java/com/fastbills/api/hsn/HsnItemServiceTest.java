package com.fastbills.api.hsn;

import com.fastbills.api.hsn.dto.CreateHsnItemRequest;
import com.fastbills.api.hsn.dto.HsnItemResponse;
import com.fastbills.api.hsn.dto.HsnLookupResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HsnItemServiceTest {

    @Mock
    private HsnItemRepository repository;

    @Mock
    private HsnValidationService validationService;

    @InjectMocks
    private HsnItemService service;

    private final UUID orgId = UUID.randomUUID();

    @BeforeEach
    void setUp() {}

    @Test
    @DisplayName("Create HSN: Successfully creates HSN item with auto-populated details")
    void testCreateHsn_Success() {
        CreateHsnItemRequest req = new CreateHsnItemRequest(
                "998313", null, null, new BigDecimal("18.00"), true, true
        );

        when(repository.existsByOrganisationIdAndCode(orgId, "998313")).thenReturn(false);
        when(validationService.lookup("998313")).thenReturn(
                new HsnLookupResponse("998313", "IT Software Services", "Software services description", new BigDecimal("18.00"), true, true)
        );

        HsnItem saved = new HsnItem(
                orgId, "998313", "IT Software Services", "Software services description", new BigDecimal("18.00"), true, true
        );
        saved.setId(UUID.randomUUID());
        when(repository.save(any(HsnItem.class))).thenReturn(saved);

        HsnItemResponse resp = service.create(orgId, req);

        assertThat(resp).isNotNull();
        assertThat(resp.code()).isEqualTo("998313");
        assertThat(resp.name()).isEqualTo("IT Software Services");
        assertThat(resp.taxRatePercent()).isEqualByComparingTo("18.00");
        assertThat(resp.isDefault()).isTrue();
        verify(repository).clearDefault(orgId);
    }

    @Test
    @DisplayName("Create HSN: Rejects duplicate code within same organisation")
    void testCreateHsn_DuplicateCode_ThrowsConflict() {
        CreateHsnItemRequest req = new CreateHsnItemRequest(
                "998313", "Name", "Desc", new BigDecimal("18.00"), true, false
        );

        when(repository.existsByOrganisationIdAndCode(orgId, "998313")).thenReturn(true);

        assertThatThrownBy(() -> service.create(orgId, req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Lookup HSN: Validates CBIC master for known code")
    void testLookup_MasterValidation() {
        HsnValidationService validator = new HsnValidationService();
        HsnLookupResponse resp = validator.lookup("998313");

        assertThat(resp.isValid()).isTrue();
        assertThat(resp.name()).contains("Information Technology");
        assertThat(resp.defaultTaxRatePercent()).isEqualByComparingTo("18.00");
        assertThat(resp.isService()).isTrue();
    }
}
