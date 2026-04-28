package com.hjo2oa.data.report.application;

import com.hjo2oa.data.report.domain.ReportDefinition;
import com.hjo2oa.data.report.domain.ReportDefinitionPage;
import com.hjo2oa.data.report.domain.ReportDefinitionQuery;
import com.hjo2oa.data.report.domain.ReportDefinitionRepository;
import com.hjo2oa.data.report.domain.ReportStatus;
import com.hjo2oa.shared.kernel.BizException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReportDefinitionApplicationService {

    private final ReportDefinitionRepository reportDefinitionRepository;
    private final Clock clock;
    @Autowired
    public ReportDefinitionApplicationService(ReportDefinitionRepository reportDefinitionRepository) {
        this(reportDefinitionRepository, Clock.systemUTC());
    }
    public ReportDefinitionApplicationService(
            ReportDefinitionRepository reportDefinitionRepository,
            Clock clock
    ) {
        this.reportDefinitionRepository = Objects.requireNonNull(
                reportDefinitionRepository,
                "reportDefinitionRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public ReportDefinition create(SaveReportDefinitionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        reportDefinitionRepository.findByCode(command.code()).ifPresent(existing -> {
            throw new BizException(
                    ReportErrorDescriptors.REPORT_DEFINITION_CONFLICT,
                    "report code already exists: " + command.code()
            );
        });
        try {
            Instant now = clock.instant();
            ReportDefinition draft = ReportDefinition.draft(
                    command.code(),
                    command.name(),
                    command.reportType(),
                    command.sourceScope(),
                    command.refreshMode(),
                    command.visibilityMode(),
                    command.tenantId(),
                    command.caliberDefinition(),
                    command.refreshConfig(),
                    command.cardProtocol(),
                    command.metrics(),
                    command.dimensions(),
                    now
            );
            return reportDefinitionRepository.save(draft);
        } catch (IllegalArgumentException ex) {
            throw new BizException(ReportErrorDescriptors.REPORT_RULE_VIOLATION, ex.getMessage(), ex);
        }
    }

    public ReportDefinition update(String code, SaveReportDefinitionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        ReportDefinition existing = getByCode(code);
        try {
            ReportDefinition updated = existing.withNewVersion(
                    command.name(),
                    command.reportType(),
                    command.sourceScope(),
                    command.refreshMode(),
                    command.visibilityMode(),
                    command.caliberDefinition(),
                    command.refreshConfig(),
                    command.cardProtocol(),
                    command.metrics(),
                    command.dimensions(),
                    clock.instant()
            );
            return reportDefinitionRepository.save(updated);
        } catch (IllegalArgumentException ex) {
            throw new BizException(ReportErrorDescriptors.REPORT_RULE_VIOLATION, ex.getMessage(), ex);
        }
    }

    public ReportDefinition changeStatus(String code, ReportStatus status) {
        ReportDefinition existing = getByCode(code);
        ReportDefinition target = switch (status) {
            case ACTIVE -> existing.activate(clock.instant());
            case ARCHIVED -> existing.archive(clock.instant());
            case DRAFT -> existing.withNewVersion(
                    existing.name(),
                    existing.reportType(),
                    existing.sourceScope(),
                    existing.refreshMode(),
                    existing.visibilityMode(),
                    existing.caliberDefinition(),
                    existing.refreshConfig(),
                    existing.cardProtocol(),
                    existing.metrics(),
                    existing.dimensions(),
                    clock.instant()
            );
        };
        return reportDefinitionRepository.save(target);
    }

    public ReportDefinition getByCode(String code) {
        return reportDefinitionRepository.findByCode(code)
                .orElseThrow(() -> new BizException(
                        ReportErrorDescriptors.REPORT_NOT_FOUND,
                        "report definition not found: " + code
                ));
    }

    public ReportDefinitionPage page(ReportDefinitionQuery query) {
        return reportDefinitionRepository.page(query);
    }
}
