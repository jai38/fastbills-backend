package com.fastbills.api.invoice;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TenantId;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @TenantId
    @Column(name = "organisation_id", nullable = false)
    private UUID organisationId;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_json", columnDefinition = "jsonb")
    private String beforeJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_json", columnDefinition = "jsonb")
    private String afterJson;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public AuditLog() {}

    public AuditLog(
            UUID organisationId,
            UUID actorUserId,
            String action,
            String entityType,
            UUID entityId,
            String beforeJson,
            String afterJson,
            String ipAddress
    ) {
        this.organisationId = organisationId;
        this.actorUserId = actorUserId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.beforeJson = beforeJson;
        this.afterJson = afterJson;
        this.ipAddress = ipAddress;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganisationId() {
        return organisationId;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public String getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public String getBeforeJson() {
        return beforeJson;
    }

    public String getAfterJson() {
        return afterJson;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
