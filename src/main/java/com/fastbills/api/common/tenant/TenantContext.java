package com.fastbills.api.common.tenant;

import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setCurrentTenant(UUID organisationId) {
        CURRENT_TENANT.set(organisationId);
    }

    public static UUID getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    public static UUID getRequiredTenant() {
        UUID tenantId = CURRENT_TENANT.get();
        if (tenantId == null) {
            throw new IllegalStateException("TenantContext is empty - query must be executed within authenticated tenant scope");
        }
        return tenantId;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
