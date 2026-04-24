package com.hjo2oa.portal.portal.model.infrastructure;

import com.hjo2oa.portal.portal.model.domain.PortalModelContext;
import com.hjo2oa.portal.portal.model.domain.PortalModelContextProvider;
import org.springframework.stereotype.Component;

@Component
public class StaticPortalModelContextProvider implements PortalModelContextProvider {

    @Override
    public PortalModelContext currentContext() {
        return new PortalModelContext("tenant-1", "portal-admin");
    }
}
