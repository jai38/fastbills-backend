package com.fastbills.api.organisation;

import com.fastbills.api.auth.dto.AuthResponse;
import com.fastbills.api.auth.dto.RegisterRequest;
import com.fastbills.api.organisation.dto.UpdateOrganisationProfileRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrganisationProfileIntegrationTest {

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
                "Profile Test Org " + unique,
                "PTO",
                "pto_" + unique + "@test.com",
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
        return headers;
    }

    @Test
    void testUpdateOrganisationProfile_Success() {
        UpdateOrganisationProfileRequest request = new UpdateOrganisationProfileRequest(
                "Acme Global Enterprises Pvt Ltd",
                "Acme Global",
                "27ABCDE1234F1Z5",
                "ABCDE1234F",
                "101, Business Park",
                "Andheri East",
                "Mumbai",
                "27",
                "400069",
                "India",
                "9820098200",
                "finance@acmeglobal.com",
                "AGE",
                "Acme Global Enterprises",
                "00112233445566",
                "HDFC0001234",
                "HDFC Bank",
                "Andheri East",
                "acmeglobal@hdfcbank",
                "Standard 30 days payment terms apply.",
                30
        );

        HttpHeaders headers = createAuthHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UpdateOrganisationProfileRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Organisation> response = restTemplate.exchange(
                "/api/v1/organisation/profile",
                HttpMethod.PUT,
                entity,
                Organisation.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Organisation body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getLegalName()).isEqualTo("Acme Global Enterprises Pvt Ltd");
        assertThat(body.getTradeName()).isEqualTo("Acme Global");
        assertThat(body.getInvoicePrefix()).isEqualTo("AGE");
        assertThat(body.getBankIfsc()).isEqualTo("HDFC0001234");
        assertThat(body.getUpiId()).isEqualTo("acmeglobal@hdfcbank");
    }

    @Test
    void testUpdateOrganisationProfile_RejectsMismatchedGstinAndState() {
        UpdateOrganisationProfileRequest request = new UpdateOrganisationProfileRequest(
                "Acme Global Enterprises Pvt Ltd",
                null,
                "27ABCDE1234F1Z5", // Maharashtra 27
                "ABCDE1234F",
                null, null, null,
                "29", // Karnataka 29 mismatch
                null, null, null, null,
                "INV",
                null, null, null, null, null, null, null, 30
        );

        HttpHeaders headers = createAuthHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UpdateOrganisationProfileRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/organisation/profile",
                HttpMethod.PUT,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("State code must match the first 2 digits of GSTIN (27)");
    }

    @Test
    void testLogoUploadAndFetchLifecycle() {
        byte[] dummyPngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        ByteArrayResource fileResource = new ByteArrayResource(dummyPngBytes) {
            @Override
            public String getFilename() {
                return "logo.png";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", fileResource);

        HttpHeaders headers = createAuthHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

        // 1. Upload logo
        ResponseEntity<Map> uploadRes = restTemplate.postForEntity(
                "/api/v1/organisation/logo",
                entity,
                Map.class
        );
        assertThat(uploadRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(uploadRes.getBody()).containsKey("objectKey");

        // 2. Fetch logo
        HttpEntity<?> getEntity = new HttpEntity<>(createAuthHeaders());
        ResponseEntity<byte[]> getRes = restTemplate.exchange(
                "/api/v1/organisation/logo",
                HttpMethod.GET,
                getEntity,
                byte[].class
        );
        assertThat(getRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getRes.getBody()).isEqualTo(dummyPngBytes);

        // 3. Delete logo
        ResponseEntity<Void> deleteRes = restTemplate.exchange(
                "/api/v1/organisation/logo",
                HttpMethod.DELETE,
                getEntity,
                Void.class
        );
        assertThat(deleteRes.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // 4. Verify logo now returns 404
        ResponseEntity<String> afterDeleteRes = restTemplate.exchange(
                "/api/v1/organisation/logo",
                HttpMethod.GET,
                getEntity,
                String.class
        );
        assertThat(afterDeleteRes.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
