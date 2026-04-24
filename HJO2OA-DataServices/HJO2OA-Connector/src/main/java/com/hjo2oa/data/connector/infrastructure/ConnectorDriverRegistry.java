package com.hjo2oa.data.connector.infrastructure;

import com.hjo2oa.data.connector.domain.ConnectorDriver;
import com.hjo2oa.data.connector.domain.ConnectorType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ConnectorDriverRegistry {

    private final Map<ConnectorType, ConnectorDriver> driversByType = new EnumMap<>(ConnectorType.class);

    public ConnectorDriverRegistry(List<ConnectorDriver> connectorDrivers) {
        for (ConnectorDriver connectorDriver : connectorDrivers) {
            driversByType.put(connectorDriver.connectorType(), connectorDriver);
        }
    }

    public Optional<ConnectorDriver> driverFor(ConnectorType connectorType) {
        return Optional.ofNullable(driversByType.get(connectorType));
    }

    public boolean supports(ConnectorType connectorType) {
        return driversByType.containsKey(connectorType);
    }
}
