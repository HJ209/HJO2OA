package com.hjo2oa.process.monitor.infrastructure;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExceptionProcessInstanceRow {
    private UUID instanceId;
    private UUID definitionId;
    private String definitionCode;
    private String title;
    private String category;
    private String status;
    private String exceptionType;
    private Long exceptionMinutes;
    private Instant detectedAt;
}
