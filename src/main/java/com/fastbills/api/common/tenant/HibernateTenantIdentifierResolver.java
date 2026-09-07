package com.fastbills.api.common.tenant;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class HibernateTenantIdentifierResolver implements CurrentTenantIdentifierResolver<UUID>, HibernatePropertiesCustomizer {

    public static final UUID NIL_TENANT_ID = new UUID(0L, 0L);

    @Override
    public UUID resolveCurrentTenantIdentifier() {
        UUID current = TenantContext.getCurrentTenant();
        return current != null ? current : NIL_TENANT_ID;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}
