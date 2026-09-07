package com.fastbills.api.customer;

import com.fastbills.api.common.tenant.TenantContext;
import com.fastbills.api.customer.dto.CreateCustomerRequest;
import com.fastbills.api.customer.dto.CustomerResponse;
import com.fastbills.api.customer.dto.UpdateCustomerRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

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
    void testCreateCustomer_Success() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "Acme Client Pvt Ltd",
                "Acme",
                "27ABCDE1234F1Z5",
                "ABCDE1234F",
                true,
                "Line 1", "Line 2", "Mumbai", "27", "400001",
                null, null, null, null, null,
                "Contact Person", "9876543210", "client@acme.com",
                "27", 30, "Notes"
        );

        when(customerRepository.existsByOrganisationIdAndLegalNameIgnoreCase(tenantId, "Acme Client Pvt Ltd"))
                .thenReturn(false);

        Customer mockCustomer = new Customer();
        mockCustomer.setId(UUID.randomUUID());
        mockCustomer.setOrganisationId(tenantId);
        mockCustomer.setLegalName("Acme Client Pvt Ltd");
        mockCustomer.setBillingStateCode("27");
        mockCustomer.setCreditDays(30);
        mockCustomer.setIsArchived(false);

        when(customerRepository.save(any(Customer.class))).thenReturn(mockCustomer);

        CustomerResponse response = customerService.createCustomer(request);

        assertThat(response).isNotNull();
        assertThat(response.legalName()).isEqualTo("Acme Client Pvt Ltd");
        assertThat(response.billingStateCode()).isEqualTo("27");
        assertThat(response.isArchived()).isFalse();
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    void testCreateCustomer_RejectsDuplicateLegalNameWithinTenant() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "Duplicate Client", null, null, null, false,
                null, null, null, "27", null, null, null, null, null, null, null, null, null, null, 30, null
        );

        when(customerRepository.existsByOrganisationIdAndLegalNameIgnoreCase(tenantId, "Duplicate Client"))
                .thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already exists");

        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void testCreateCustomer_RejectsMismatchedGstinAndBillingState() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "Valid Client", null,
                "27ABCDE1234F1Z5", // Maharashtra 27
                null, false,
                null, null, null,
                "29", // Karnataka 29 mismatch
                null, null, null, null, null, null, null, null, null, null, 30, null
        );

        when(customerRepository.existsByOrganisationIdAndLegalNameIgnoreCase(tenantId, "Valid Client"))
                .thenReturn(false);

        assertThatThrownBy(() -> customerService.createCustomer(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Billing state code (29) must match first 2 digits of GSTIN (27)");

        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void testCreateCustomer_RejectsRegisteredWithoutGstin() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "Registered Client", null,
                null, // No GSTIN
                null,
                true, // Marked registered
                null, null, null, "27", null, null, null, null, null, null, null, null, null, null, 30, null
        );

        when(customerRepository.existsByOrganisationIdAndLegalNameIgnoreCase(tenantId, "Registered Client"))
                .thenReturn(false);

        assertThatThrownBy(() -> customerService.createCustomer(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("GSTIN is required when customer is marked as registered under GST");

        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void testArchiveAndUnarchiveCustomer() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setOrganisationId(tenantId);
        customer.setLegalName("Active Customer");
        customer.setBillingStateCode("27");
        customer.setIsArchived(false);

        when(customerRepository.findByIdAndOrganisationId(customerId, tenantId))
                .thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Archive
        CustomerResponse archived = customerService.archiveCustomer(customerId);
        assertThat(archived.isArchived()).isTrue();

        // Unarchive
        CustomerResponse unarchived = customerService.unarchiveCustomer(customerId);
        assertThat(unarchived.isArchived()).isFalse();
    }
}
