package com.hjo2oa.data.service.infrastructure;

import com.hjo2oa.data.service.application.DataServiceExecutionGateway;
import com.hjo2oa.data.service.application.DataServiceExecutionRequest;
import com.hjo2oa.data.service.application.DataServiceExecutionResult;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class EchoDataServiceExecutionGateway implements DataServiceExecutionGateway {

    private final Clock clock;

    public EchoDataServiceExecutionGateway() {
        this(Clock.systemUTC());
    }

    EchoDataServiceExecutionGateway(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public DataServiceExecutionResult execute(DataServiceExecutionRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestId", request.requestId());
        payload.put("clientCode", request.clientCode());
        payload.put("serviceId", request.definition().serviceId());
        payload.put("serviceCode", request.definition().code());
        payload.put("serviceName", request.definition().name());
        payload.put("serviceType", request.definition().serviceType().name());
        payload.put("permissionMode", request.definition().permissionMode().name());
        payload.put("queryParameters", request.queryParameters());
        payload.put("requestBody", request.requestBody());
        payload.put("executedAt", clock.instant().toString());
        return new DataServiceExecutionResult(payload);
    }
}
