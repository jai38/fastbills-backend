package com.fastbills.api.customer;

import com.fastbills.api.auth.dto.AuthResponse;
import com.fastbills.api.auth.dto.RegisterRequest;
import com.fastbills.api.customer.dto.CreateCustomerRequest;
import com.fastbills.api.customer.dto.CustomerResponse;
import com.fastbills.api.customer.dto.UpdateCustomerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class CustomerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    private String token;
    private UUID orgId;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        RegisterRequest registerRequest = new RegisterRequest(
                "Customer Test Org " + unique,
                "CTO",
                "owner_" + unique + "@cto.com",
                "Password123!",
                "Owner " + unique,
                "27ABCDE1234F1Z5",
                "27",
                "ABCDE1234F"
        );
        ResponseEntity<AuthResponse> authResponse = restTemplate.postForEntity(
                "/api/v1/auth/register",
                registerRequest,
                AuthResponse.class
        );
        assertThat(authResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        this.token = authResponse.getBody().accessToken();
        this.orgId = authResponse.getBody().organisation().id();
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    void testCreateCustomer_Success() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "Tata Motors Limited",
                "Tata Motors",
                "27AAACT2727Q1ZW",
                "AAACT2727Q",
                true,
                "Bombay House, 24 Homi Mody Street",
                "Fort",
                "Mumbai",
                "27",
                "400001",
                null,
                null,
                null,
                null,
                null,
                "Mr. Sharma",
                "9876543210",
                "procurement@tatamotors.com",
                "27",
                45,
                "Key enterprise client"
        );

        HttpEntity<CreateCustomerRequest> entity = new HttpEntity<>(request, createAuthHeaders());
        ResponseEntity<CustomerResponse> response = restTemplate.postForEntity(
                "/api/v1/customers",
                entity,
                CustomerResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        CustomerResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.id()).isNotNull();
        assertThat(body.organisationId()).isEqualTo(orgId);
        assertThat(body.legalName()).isEqualTo("Tata Motors Limited");
        assertThat(body.tradeName()).isEqualTo("Tata Motors");
        assertThat(body.billingStateCode()).isEqualTo("27");
        assertThat(body.creditDays()).isEqualTo(45);
        assertThat(body.isArchived()).isFalse();
    }

    @Test
    void testCreateCustomer_RejectsDuplicateLegalNameWithinOrg() {
        CreateCustomerRequest request1 = new CreateCustomerRequest(
                "Reliance Industries Limited",
                null, null, null, false, null, null, null, "27", null, null, null, null, null, null, null, null, null, null, 30, null
        );
        HttpEntity<CreateCustomerRequest> entity1 = new HttpEntity<>(request1, createAuthHeaders());
        ResponseEntity<CustomerResponse> response1 = restTemplate.postForEntity("/api/v1/customers", entity1, CustomerResponse.class);
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Case-insensitive duplicate check
        CreateCustomerRequest request2 = new CreateCustomerRequest(
                "reliance industries limited",
                null, null, null, false, null, null, null, "27", null, null, null, null, null, null, null, null, null, null, 30, null
        );
        HttpEntity<CreateCustomerRequest> entity2 = new HttpEntity<>(request2, createAuthHeaders());
        ResponseEntity<String> response2 = restTemplate.postForEntity("/api/v1/customers", entity2, String.class);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response2.getBody()).contains("DUPLICATE_RESOURCE");
    }

    @Test
    void testCreateCustomer_RejectsMismatchedGstinAndBillingState() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "Infosys Limited",
                null,
                "29AABCI1234F1Z5", // Karnataka 29
                null,
                false,
                null, null, null,
                "27", // Maharashtra 27 mismatch
                null, null, null, null, null, null, null, null, null, null, 30, null
        );

        HttpEntity<CreateCustomerRequest> entity = new HttpEntity<>(request, createAuthHeaders());
        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/customers", entity, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Billing state code (27) must match first 2 digits of GSTIN (29)");
    }

    @Test
    void testCreateCustomer_RegisteredCustomerRequiresGstin() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "Wipro Limited",
                null,
                null, // missing GSTIN
                null,
                true, // marked registered
                null, null, null,
                "29",
                null, null, null, null, null, null, null, null, null, null, 30, null
        );

        HttpEntity<CreateCustomerRequest> entity = new HttpEntity<>(request, createAuthHeaders());
        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/customers", entity, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("GSTIN is required when customer is marked as registered under GST");
    }

    @Test
    void testCustomerUpdateAndArchivalLifecycle() {
        CreateCustomerRequest createReq = new CreateCustomerRequest(
                "Larsen & Toubro",
                "L&T",
                null, null, false,
                "L&T House, Ballard Estate", null, "Mumbai", "27", "400001",
                null, null, null, null, null, null, null, null, null, 30, null
        );
        HttpEntity<CreateCustomerRequest> createEntity = new HttpEntity<>(createReq, createAuthHeaders());
        CustomerResponse created = restTemplate.postForEntity("/api/v1/customers", createEntity, CustomerResponse.class).getBody();
        assertThat(created).isNotNull();
        UUID customerId = created.id();

        // 1. Update customer
        UpdateCustomerRequest updateReq = new UpdateCustomerRequest(
                "Larsen & Toubro Ltd",
                "L&T ECC Division",
                null, null, false,
                "Powai Campus", null, "Mumbai", "27", "400072",
                null, null, null, null, null, "John Doe", "9988776655", "contact@lnt.com", null, 45, "Updated notes"
        );
        HttpEntity<UpdateCustomerRequest> updateEntity = new HttpEntity<>(updateReq, createAuthHeaders());
        ResponseEntity<CustomerResponse> updateRes = restTemplate.exchange(
                "/api/v1/customers/" + customerId,
                HttpMethod.PUT,
                updateEntity,
                CustomerResponse.class
        );
        assertThat(updateRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateRes.getBody().legalName()).isEqualTo("Larsen & Toubro Ltd");
        assertThat(updateRes.getBody().creditDays()).isEqualTo(45);

        // 2. Archive customer
        HttpEntity<?> emptyEntity = new HttpEntity<>(createAuthHeaders());
        ResponseEntity<CustomerResponse> archiveRes = restTemplate.exchange(
                "/api/v1/customers/" + customerId + "/archive",
                HttpMethod.POST,
                emptyEntity,
                CustomerResponse.class
        );
        assertThat(archiveRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(archiveRes.getBody().isArchived()).isTrue();

        // 3. Unarchive customer
        ResponseEntity<CustomerResponse> unarchiveRes = restTemplate.exchange(
                "/api/v1/customers/" + customerId + "/unarchive",
                HttpMethod.POST,
                emptyEntity,
                CustomerResponse.class
        );
        assertThat(unarchiveRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(unarchiveRes.getBody().isArchived()).isFalse();
    }
}
