package com.fastbills.api.auth;

import com.fastbills.api.auth.dto.AuthResponse;
import com.fastbills.api.auth.dto.LoginRequest;
import com.fastbills.api.auth.dto.RefreshTokenRequest;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testRegistration_Success() {
        RegisterRequest request = new RegisterRequest(
                "Acme Fabrications Ltd",
                "Acme Fab",
                "owner@acmefab.com",
                "SecurePass123!",
                "Ramesh Kumar",
                "27ABCDE1234F1Z5",
                "27",
                "ABCDE1234F"
        );

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/register",
                request,
                AuthResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        AuthResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.accessToken()).isNotBlank();
        assertThat(body.refreshToken()).isNotBlank();
        assertThat(body.user().email()).isEqualTo("owner@acmefab.com");
        assertThat(body.user().role().name()).isEqualTo("OWNER");
        assertThat(body.organisation().legalName()).isEqualTo("Acme Fabrications Ltd");
        assertThat(body.organisation().gstin()).isEqualTo("27ABCDE1234F1Z5");
        assertThat(body.organisation().stateCode()).isEqualTo("27");
    }

    @Test
    void testRegistration_FailsWhenStateCodeMismatchesGstin() {
        RegisterRequest request = new RegisterRequest(
                "Mismatched State Corp",
                null,
                "mismatch@test.com",
                "SecurePass123!",
                "Suresh Patel",
                "27ABCDE1234F1Z5", // Maharashtra
                "29",             // Karnataka - mismatch!
                null
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/auth/register",
                request,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("must match first two characters of GSTIN");
    }

    @Test
    void testRegistration_FailsOnDuplicateEmail() {
        RegisterRequest request = new RegisterRequest(
                "Alpha Corp",
                null,
                "duplicate@alpha.com",
                "SecurePass123!",
                "Alpha Owner",
                null,
                null,
                null
        );

        ResponseEntity<AuthResponse> response1 = restTemplate.postForEntity(
                "/api/v1/auth/register",
                request,
                AuthResponse.class
        );
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Attempt second registration with same email
        RegisterRequest request2 = new RegisterRequest(
                "Beta Corp",
                null,
                "DUPLICATE@alpha.com", // Case-insensitive duplicate
                "SecurePass123!",
                "Beta Owner",
                null,
                null,
                null
        );

        ResponseEntity<String> response2 = restTemplate.postForEntity(
                "/api/v1/auth/register",
                request2,
                String.class
        );
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response2.getBody()).contains("Email is already registered");
    }

    @Test
    void testLoginAndTokenRefresh() {
        // Register user
        RegisterRequest regRequest = new RegisterRequest(
                "Login Test Org",
                null,
                "login@test.com",
                "Password123!",
                "Login User",
                null,
                null,
                null
        );
        restTemplate.postForEntity("/api/v1/auth/register", regRequest, AuthResponse.class);

        // Login with correct password
        LoginRequest loginRequest = new LoginRequest("login@test.com", "Password123!");
        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login",
                loginRequest,
                AuthResponse.class
        );

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        AuthResponse authBody = loginResponse.getBody();
        assertThat(authBody).isNotNull();
        String accessToken = authBody.accessToken();
        String refreshToken = authBody.refreshToken();

        // Access protected /api/v1/auth/me
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> meResponse = restTemplate.exchange(
                "/api/v1/auth/me",
                HttpMethod.GET,
                entity,
                String.class
        );
        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meResponse.getBody()).contains("login@test.com");

        // Refresh token exchange
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);
        ResponseEntity<AuthResponse> refreshResponse = restTemplate.postForEntity(
                "/api/v1/auth/refresh",
                refreshRequest,
                AuthResponse.class
        );

        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshResponse.getBody()).isNotNull();
        assertThat(refreshResponse.getBody().accessToken()).isNotBlank();
        assertThat(refreshResponse.getBody().refreshToken()).isNotEqualTo(refreshToken); // Token rotated
    }
}
