package com.hjo2oa.data.service.infrastructure;

import com.hjo2oa.data.service.domain.DataServiceOperationContext;
import com.hjo2oa.data.service.domain.DataServiceOperationContextProvider;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class StaticDataServiceOperationContextProvider implements DataServiceOperationContextProvider {

    private static final UUID DEFAULT_TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Override
    public DataServiceOperationContext currentContext() {
        return new DataServiceOperationContext(
                DEFAULT_TENANT_ID,
                "data-service-admin",
                Set.of(
                        DataServiceOperationContext.ROLE_PLATFORM_ADMIN,
                        DataServiceOperationContext.ROLE_DATA_SERVICE_MANAGER,
                        DataServiceOperationContext.ROLE_DATA_SERVICE_AUDITOR
                ),
                Set.of("open-api", "report", "data-sync"),
                Set.of("subject-1", "subject-2"),
                true
        );
    }
}
