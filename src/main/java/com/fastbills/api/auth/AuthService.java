package com.fastbills.api.auth;

import com.fastbills.api.auth.dto.AuthResponse;
import com.fastbills.api.auth.dto.LoginRequest;
import com.fastbills.api.auth.dto.RefreshTokenRequest;
import com.fastbills.api.auth.dto.RegisterRequest;
import com.fastbills.api.common.error.ErrorCode;
import com.fastbills.api.organisation.Organisation;
import com.fastbills.api.organisation.OrganisationRepository;
import com.fastbills.api.user.User;
import com.fastbills.api.user.UserRepository;
import com.fastbills.api.user.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {

    private final OrganisationRepository organisationRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            OrganisationRepository organisationRepository,
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.organisationRepository = organisationRepository;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase().trim();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email is already registered: " + email);
        }

        String gstin = request.gstin() != null ? request.gstin().trim().toUpperCase() : null;
        String stateCode = request.stateCode() != null ? request.stateCode().trim() : null;

        if (gstin != null) {
            String gstinState = gstin.substring(0, 2);
            if (stateCode != null && !stateCode.equals(gstinState)) {
                throw new IllegalArgumentException("State code (" + stateCode + ") must match first two characters of GSTIN (" + gstinState + ")");
            }
            stateCode = gstinState;
        }

        Organisation org = new Organisation(request.legalName().trim());
        org.setTradeName(request.tradeName() != null ? request.tradeName().trim() : null);
        org.setGstin(gstin);
        org.setStateCode(stateCode);
        org.setPan(request.pan() != null ? request.pan().trim().toUpperCase() : null);
        org.setEmail(email);

        Organisation savedOrg = organisationRepository.save(org);

        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(
                savedOrg.getId(),
                email,
                passwordHash,
                request.fullName().trim(),
                UserRole.OWNER
        );
        User savedUser = userRepository.save(user);

        return createAuthResponse(savedUser, savedOrg);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.email().toLowerCase().trim();
        User user = userRepository.findByEmailForAuthentication(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalArgumentException("User account is inactive");
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        Organisation org = organisationRepository.findById(user.getOrganisationId())
                .orElseThrow(() -> new IllegalStateException("Organisation not found for user: " + user.getOrganisationId()));

        return createAuthResponse(user, org);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String rawToken = request.refreshToken();
        String hash = hashToken(rawToken);

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired refresh token"));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new IllegalArgumentException("Refresh token has expired");
        }

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new IllegalStateException("User not found for refresh token"));

        Organisation org = organisationRepository.findById(user.getOrganisationId())
                .orElseThrow(() -> new IllegalStateException("Organisation not found for user"));

        // Rotate token: delete old, create new
        refreshTokenRepository.delete(refreshToken);

        return createAuthResponse(user, org);
    }

    private AuthResponse createAuthResponse(User user, Organisation org) {
        String accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = UUID.randomUUID().toString();
        String tokenHash = hashToken(rawRefreshToken);

        RefreshToken refreshTokenEntity = new RefreshToken(
                user.getId(),
                tokenHash,
                jwtService.getRefreshExpiry()
        );
        refreshTokenRepository.save(refreshTokenEntity);

        return new AuthResponse(
                accessToken,
                rawRefreshToken,
                "Bearer",
                3600,
                new AuthResponse.UserDto(user.getId(), user.getEmail(), user.getFullName(), user.getRole()),
                new AuthResponse.OrganisationDto(org.getId(), org.getLegalName(), org.getTradeName(), org.getGstin(), org.getStateCode())
        );
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
