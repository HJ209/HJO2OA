package com.hjo2oa.data.connector.interfaces;

import com.hjo2oa.data.connector.application.ConnectorDefinitionApplicationService;
import com.hjo2oa.data.connector.domain.ConnectorDefinitionView;
import com.hjo2oa.data.connector.domain.ConnectorHealthOverviewView;
import com.hjo2oa.data.connector.domain.ConnectorHealthStatus;
import com.hjo2oa.data.connector.domain.ConnectorHealthSnapshotView;
import com.hjo2oa.data.connector.domain.ConnectorListView;
import com.hjo2oa.data.connector.domain.ConnectorParameterTemplate;
import com.hjo2oa.data.connector.domain.ConnectorParameterTemplateCategory;
import com.hjo2oa.data.connector.domain.ConnectorStatus;
import com.hjo2oa.data.connector.domain.ConnectorType;
import com.hjo2oa.data.connector.infrastructure.ApiIdempotencyStore;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/data/connectors")
public class ConnectorController {

    static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";

    private final ConnectorDefinitionApplicationService applicationService;
    private final ResponseMetaFactory responseMetaFactory;
    private final ApiIdempotencyStore apiIdempotencyStore;

    public ConnectorController(
            ConnectorDefinitionApplicationService applicationService,
            ResponseMetaFactory responseMetaFactory,
            ApiIdempotencyStore apiIdempotencyStore
    ) {
        this.applicationService = applicationService;
        this.responseMetaFactory = responseMetaFactory;
        this.apiIdempotencyStore = apiIdempotencyStore;
    }

    @GetMapping
    public ApiResponse<ConnectorListView> list(
            @RequestParam(required = false) ConnectorType connectorType,
            @RequestParam(required = false) ConnectorStatus status,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            HttpServletRequest request
    ) {
        ConnectorListView data = applicationService.list(connectorType, status, code, keyword, page, size);
        return ApiResponse.success(data, responseMetaFactory.create(request));
    }

    @GetMapping("/{connectorId}")
    public ApiResponse<ConnectorDefinitionView> current(
            @PathVariable String connectorId,
            HttpServletRequest request
    ) {
        ConnectorDefinitionView connectorDefinition = applicationService.current(connectorId)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "连接器不存在"));
        return ApiResponse.success(connectorDefinition, responseMetaFactory.create(request));
    }

    @GetMapping("/{connectorId}/parameter-templates")
    public ApiResponse<List<ConnectorParameterTemplate>> parameterTemplates(
            @PathVariable String connectorId,
            @RequestParam(required = false) ConnectorParameterTemplateCategory category,
            @RequestParam(required = false) Boolean sensitive,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.parameterTemplates(connectorId, category, sensitive),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/{connectorId}/tests/latest")
    public ApiResponse<ConnectorHealthSnapshotView> latestTest(
            @PathVariable String connectorId,
            HttpServletRequest request
    ) {
        ConnectorHealthSnapshotView latestTestSnapshot = applicationService.latestTestSnapshot(connectorId)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "暂无连接器测试结果"));
        return ApiResponse.success(latestTestSnapshot, responseMetaFactory.create(request));
    }

    @GetMapping("/{connectorId}/tests/history")
    public ApiResponse<List<ConnectorHealthSnapshotView>> recentTestHistory(
            @PathVariable String connectorId,
            @RequestParam(required = false) ConnectorHealthStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant checkedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant checkedTo,
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.recentTestSnapshots(connectorId, status, checkedFrom, checkedTo, limit == null ? 10 : limit),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/{connectorId}/health")
    public ApiResponse<ConnectorHealthOverviewView> latestHealth(
            @PathVariable String connectorId,
            HttpServletRequest request
    ) {
        ConnectorHealthOverviewView latestHealthOverview = applicationService.latestHealthOverview(connectorId)
                .orElseGet(() -> new ConnectorHealthOverviewView(connectorId, null, null, 0, 0, 0, 0));
        return ApiResponse.success(latestHealthOverview, responseMetaFactory.create(request));
    }

    @GetMapping("/{connectorId}/health/history")
    public ApiResponse<List<ConnectorHealthSnapshotView>> recentHealthHistory(
            @PathVariable String connectorId,
            @RequestParam(required = false) ConnectorHealthStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant checkedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant checkedTo,
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.recentHealthSnapshots(connectorId, status, checkedFrom, checkedTo,
                        limit == null ? 10 : limit),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{connectorId}")
    public ResponseEntity<ApiResponse<ConnectorDefinitionView>> upsert(
            @PathVariable String connectorId,
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @Valid @RequestBody UpsertConnectorDefinitionRequest body,
            HttpServletRequest request
    ) {
        return apiIdempotencyStore.execute(
                "connector:upsert:" + connectorId,
                requiredIdempotencyKey(idempotencyKey),
                () -> ApiResponse.success(
                        applicationService.upsert(body.toCommand(connectorId)),
                        responseMetaFactory.create(request)
                )
        );
    }

    @PutMapping("/{connectorId}/parameters")
    public ResponseEntity<ApiResponse<ConnectorDefinitionView>> configureParameters(
            @PathVariable String connectorId,
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @Valid @RequestBody ConfigureConnectorParametersRequest body,
            HttpServletRequest request
    ) {
        return apiIdempotencyStore.execute(
                "connector:parameters:" + connectorId,
                requiredIdempotencyKey(idempotencyKey),
                () -> ApiResponse.success(
                        applicationService.configureParameters(body.toCommand(connectorId)),
                        responseMetaFactory.create(request)
                )
        );
    }

    @PostMapping("/{connectorId}/activate")
    public ResponseEntity<ApiResponse<ConnectorDefinitionView>> activate(
            @PathVariable String connectorId,
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            HttpServletRequest request
    ) {
        return apiIdempotencyStore.execute(
                "connector:activate:" + connectorId,
                requiredIdempotencyKey(idempotencyKey),
                () -> ApiResponse.success(
                        applicationService.activate(connectorId),
                        responseMetaFactory.create(request)
                )
        );
    }

    @PostMapping("/{connectorId}/disable")
    public ResponseEntity<ApiResponse<ConnectorDefinitionView>> disable(
            @PathVariable String connectorId,
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            HttpServletRequest request
    ) {
        return apiIdempotencyStore.execute(
                "connector:disable:" + connectorId,
                requiredIdempotencyKey(idempotencyKey),
                () -> ApiResponse.success(
                        applicationService.disable(connectorId),
                        responseMetaFactory.create(request)
                )
        );
    }

    @PostMapping("/{connectorId}/test")
    public ResponseEntity<ApiResponse<ConnectorHealthSnapshotView>> testConnection(
            @PathVariable String connectorId,
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            HttpServletRequest request
    ) {
        return apiIdempotencyStore.execute(
                "connector:test:" + connectorId,
                requiredIdempotencyKey(idempotencyKey),
                () -> ApiResponse.success(
                        applicationService.testConnection(connectorId),
                        responseMetaFactory.create(request)
                )
        );
    }

    @PostMapping("/{connectorId}/health/refresh")
    public ResponseEntity<ApiResponse<ConnectorHealthSnapshotView>> refreshHealth(
            @PathVariable String connectorId,
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            HttpServletRequest request
    ) {
        return apiIdempotencyStore.execute(
                "connector:health-refresh:" + connectorId,
                requiredIdempotencyKey(idempotencyKey),
                () -> ApiResponse.success(
                        applicationService.refreshHealth(connectorId),
                        responseMetaFactory.create(request)
                )
        );
    }

    @PostMapping("/{connectorId}/health/{snapshotId}/confirm")
    public ResponseEntity<ApiResponse<ConnectorHealthSnapshotView>> confirmHealthAbnormal(
            @PathVariable String connectorId,
            @PathVariable String snapshotId,
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @Valid @RequestBody ConfirmConnectorHealthAbnormalRequest body,
            HttpServletRequest request
    ) {
        return apiIdempotencyStore.execute(
                "connector:health-confirm:" + snapshotId,
                requiredIdempotencyKey(idempotencyKey),
                () -> ApiResponse.success(
                        applicationService.confirmHealthAbnormal(connectorId, snapshotId, body.note()),
                        responseMetaFactory.create(request)
                )
        );
    }

    private String requiredIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BizException(SharedErrorDescriptors.BAD_REQUEST, "写操作必须提供 X-Idempotency-Key");
        }
        return idempotencyKey.trim();
    }
}
