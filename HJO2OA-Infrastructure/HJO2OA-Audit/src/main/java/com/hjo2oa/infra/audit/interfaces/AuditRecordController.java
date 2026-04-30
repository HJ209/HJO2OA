package com.hjo2oa.infra.audit.interfaces;

import com.hjo2oa.infra.audit.application.AuditRecordApplicationService;
import com.hjo2oa.infra.audit.domain.AuditQuery;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/infra/audits")
public class AuditRecordController {

    private final AuditRecordApplicationService applicationService;
    private final AuditRecordDtoMapper dtoMapper;
    private final ResponseMetaFactory responseMetaFactory;

    public AuditRecordController(
            AuditRecordApplicationService applicationService,
            AuditRecordDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.dtoMapper = dtoMapper;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PostMapping
    public ApiResponse<AuditRecordDtos.DetailResponse> record(
            @Valid @RequestBody AuditRecordDtos.RecordRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDetailResponse(applicationService.recordAudit(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping
    public ApiResponse<List<AuditRecordDtos.SummaryResponse>> list(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) String moduleCode,
            @RequestParam(required = false) String objectType,
            @RequestParam(required = false) String objectId,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) UUID operatorAccountId,
            @RequestParam(required = false) UUID operatorPersonId,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            HttpServletRequest request
    ) {
        String effectiveTraceId = traceId == null || traceId.isBlank() ? requestId : traceId;
        AuditQuery query = new AuditQuery(
                tenantId,
                moduleCode,
                objectType,
                objectId,
                actionType,
                operatorAccountId,
                operatorPersonId,
                effectiveTraceId,
                from,
                to
        );
        return ApiResponse.success(
                applicationService.queryAudits(query).stream().map(dtoMapper::toSummaryResponse).toList(),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/{recordId}")
    public ApiResponse<AuditRecordDtos.DetailResponse> detail(
            @PathVariable UUID recordId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDetailResponse(applicationService.getRecord(recordId)),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{recordId}/archive")
    public ApiResponse<AuditRecordDtos.DetailResponse> archive(
            @PathVariable UUID recordId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDetailResponse(applicationService.archiveRecord(recordId)),
                responseMetaFactory.create(request)
        );
    }
}
