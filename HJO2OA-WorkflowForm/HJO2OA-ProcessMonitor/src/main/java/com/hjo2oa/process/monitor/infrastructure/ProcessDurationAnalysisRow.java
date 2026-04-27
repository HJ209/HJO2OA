package com.hjo2oa.process.monitor.infrastructure;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProcessDurationAnalysisRow {

    private UUID definitionId;
    private String definitionCode;
    private String category;
    private Long instanceCount;
    private Long completedCount;
    private Long runningCount;
    private Long averageDurationMinutes;
    private Long maxDurationMinutes;
}
