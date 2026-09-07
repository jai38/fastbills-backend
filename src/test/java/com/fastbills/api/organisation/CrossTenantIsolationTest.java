package com.fastbills.api.organisation;

import com.fastbills.api.auth.dto.AuthResponse;
import com.fastbills.api.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class CrossTenantIsolationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testCrossTenantIsolation_AccessBlocked() {
        // Register Organisation A
        RegisterRequest orgARequest = new RegisterRequest(
                "Tenant A Enterprises",
                "Tenant A",
                "owner@tenant-a.com",
                "PasswordA123!",
                "Owner A",
                null,
                null,
                null
        );
        ResponseEntity<AuthResponse> orgAResponse = restTemplate.postForEntity(
                "/api/v1/auth/register",
                orgARequest,
                AuthResponse.class
        );
        assertThat(orgAResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        AuthResponse authA = orgAResponse.getBody();
        assertThat(authA).isNotNull();
        UUID orgAId = authA.organisation().id();
        String tokenA = authA.accessToken();

        // Register Organisation B
        RegisterRequest orgBRequest = new RegisterRequest(
                "Tenant B Industries",
                "Tenant B",
                "owner@tenant-b.com",
                "PasswordB123!",
                "Owner B",
                null,
                null,
                null
        );
        ResponseEntity<AuthResponse> orgBResponse = restTemplate.postForEntity(
                "/api/v1/auth/register",
                orgBRequest,
                AuthResponse.class
        );
        assertThat(orgBResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        AuthResponse authB = orgBResponse.getBody();
        assertThat(authB).isNotNull();
        UUID orgBId = authB.organisation().id();

        // As Tenant A, read Tenant A's own profile -> Expect 200 OK
        HttpHeaders headersA = new HttpHeaders();
        headersA.setBearerAuth(tokenA);
        HttpEntity<Void> entityA = new HttpEntity<>(headersA);

        ResponseEntity<Organisation> ownProfileResponse = restTemplate.exchange(
                "/api/v1/organisation/" + orgAId,
                HttpMethod.GET,
                entityA,
                Organisation.class
        );
        assertThat(ownProfileResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ownProfileResponse.getBody()).isNotNull();
        assertThat(ownProfileResponse.getBody().getLegalName()).isEqualTo("Tenant A Enterprises");

        // As Tenant A, attempt to access Tenant B's organisation by ID -> Must be blocked!
        ResponseEntity<String> crossTenantResponse = restTemplate.exchange(
                "/api/v1/organisation/" + orgBId,
                HttpMethod.GET,
                entityA,
                String.class
        );

        assertThat(crossTenantResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(crossTenantResponse.getBody()).contains("ACCESS_DENIED");
        assertThat(crossTenantResponse.getBody()).contains("Cross-tenant access prohibited");

        // --- Cross-Tenant Customer Isolation ---
        String tokenB = authB.accessToken();
        HttpHeaders headersB = new HttpHeaders();
        headersB.setBearerAuth(tokenB);
        headersB.setContentType(MediaType.APPLICATION_JSON);

        // 1. Tenant A creates customer "Shared Client Name"
        com.fastbills.api.customer.dto.CreateCustomerRequest createCustA =
                new com.fastbills.api.customer.dto.CreateCustomerRequest(
                        "Shared Client Name", null, null, null, false,
                        null, null, null, "27", null, null, null, null, null, null, null, null, null, null, 30, null
                );
        headersA.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<com.fastbills.api.customer.dto.CreateCustomerRequest> entityCustA = new HttpEntity<>(createCustA, headersA);
        ResponseEntity<com.fastbills.api.customer.dto.CustomerResponse> custAResponse = restTemplate.postForEntity(
                "/api/v1/customers",
                entityCustA,
                com.fastbills.api.customer.dto.CustomerResponse.class
        );
        assertThat(custAResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID customerAId = custAResponse.getBody().id();

        // 2. Tenant B attempts to fetch Tenant A's customer by ID -> Expect 404 NOT_FOUND
        HttpEntity<Void> entityB = new HttpEntity<>(headersB);
        ResponseEntity<String> crossCustGet = restTemplate.exchange(
                "/api/v1/customers/" + customerAId,
                HttpMethod.GET,
                entityB,
                String.class
        );
        assertThat(crossCustGet.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // 3. Tenant B attempts to update Tenant A's customer -> Expect 404 NOT_FOUND
        com.fastbills.api.customer.dto.UpdateCustomerRequest updateCustB =
                new com.fastbills.api.customer.dto.UpdateCustomerRequest(
                        "Hacked Name", null, null, null, false,
                        null, null, null, "27", null, null, null, null, null, null, null, null, null, null, 30, null
                );
        HttpEntity<com.fastbills.api.customer.dto.UpdateCustomerRequest> entityCustUpdateB = new HttpEntity<>(updateCustB, headersB);
        ResponseEntity<String> crossCustUpdate = restTemplate.exchange(
                "/api/v1/customers/" + customerAId,
                HttpMethod.PUT,
                entityCustUpdateB,
                String.class
        );
        assertThat(crossCustUpdate.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // 4. Tenant B creates a customer with the EXACT SAME legal name -> Must SUCCEED (tenant-scoped uniqueness)
        com.fastbills.api.customer.dto.CreateCustomerRequest createCustB =
                new com.fastbills.api.customer.dto.CreateCustomerRequest(
                        "Shared Client Name", null, null, null, false,
                        null, null, null, "27", null, null, null, null, null, null, null, null, null, null, 30, null
                );
        HttpEntity<com.fastbills.api.customer.dto.CreateCustomerRequest> entityCustB = new HttpEntity<>(createCustB, headersB);
        ResponseEntity<com.fastbills.api.customer.dto.CustomerResponse> custBResponse = restTemplate.postForEntity(
                "/api/v1/customers",
                entityCustB,
                com.fastbills.api.customer.dto.CustomerResponse.class
        );
        assertThat(custBResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(custBResponse.getBody().organisationId()).isEqualTo(orgBId);
    }
}
