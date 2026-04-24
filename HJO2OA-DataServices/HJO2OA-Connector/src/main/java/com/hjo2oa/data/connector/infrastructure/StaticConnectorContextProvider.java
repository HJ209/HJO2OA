package com.hjo2oa.data.connector.infrastructure;

import com.hjo2oa.data.connector.domain.ConnectorContext;
import com.hjo2oa.data.connector.domain.ConnectorContextProvider;
import org.springframework.stereotype.Component;

@Component
public class StaticConnectorContextProvider implements ConnectorContextProvider {

    @Override
    public ConnectorContext currentContext() {
        return new ConnectorContext("tenant-default", "connector-admin", "local");
    }
}
