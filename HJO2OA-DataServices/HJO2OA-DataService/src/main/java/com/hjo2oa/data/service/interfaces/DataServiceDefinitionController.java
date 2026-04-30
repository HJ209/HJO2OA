package com.hjo2oa.data.service.interfaces;

import com.hjo2oa.data.common.domain.exception.DataServicesErrorCode;
import com.hjo2oa.data.common.domain.exception.DataServicesException;
import com.hjo2oa.data.service.application.DataServiceDefinitionApplicationService;
import com.hjo2oa.data.service.application.DataServiceDefinitionCommands;
import com.hjo2oa.data.service.domain.DataServiceDefinitionRepository;
import com.hjo2oa.data.service.domain.DataServiceViews;
import com.hjo2oa.data.service.domain.ServiceParameterDefinition;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.Pagination;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping({"/api/v1/data/services/definitions", "/api/v1/data-services/definitions"})
public class DataServiceDefinitionController {

    private final DataServiceDefinitionApplicationService applicationService;
    private final DataServiceDefinitionDtoMapper dtoMapper;
    private final ResponseMetaFactory responseMetaFactory;

    public DataServiceDefinitionController(
            DataServiceDefinitionApplicationService applicationService,
            DataServiceDefinitionDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.dtoMapper = dtoMapper;
        this.responseMetaFactory = responseMetaFactory;
    }

    @GetMapping
    public ApiResponse<com.hjo2oa.shared.web.PageData<DataServiceDefinitionDtos.SummaryResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) com.hjo2oa.data.service.domain.DataServiceDefinition.ServiceType serviceType,
            @RequestParam(required = false) com.hjo2oa.data.service.domain.DataServiceDefinition.Status status,
            HttpServletRequest request
    ) {
        DataServiceDefinitionRepository.SearchResult<DataServiceViews.SummaryView> result = applicationService.list(
                new DataServiceDefinitionCommands.ListQuery(page, size, code, keyword, serviceType, status)
        );
        List<DataServiceDefinitionDtos.SummaryResponse> items = result.items().stream()
                .map(dtoMapper::toSummaryResponse)
                .toList();
        return ApiResponse.page(
                items,
                Pagination.of(page, size, result.total()),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/{serviceId}")
    public ApiResponse<DataServiceDefinitionDtos.DetailResponse> current(
            @PathVariable UUID serviceId,
            HttpServletRequest request
    ) {
        DataServiceViews.DetailView view = applicationService.current(serviceId)
                .orElseThrow(() -> new DataServicesException(
                        DataServicesErrorCode.DATA_SERVICE_NOT_FOUND,
                        "Data service definition not found"
                ));
        return ApiResponse.success(dtoMapper.toTarget(view), responseMetaFactory.create(request));
    }

    @PostMapping
    public ApiResponse<DataServiceDefinitionDtos.DetailResponse> create(
            @Valid @RequestBody DataServiceDefinitionDtos.CreateRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toTarget(applicationService.create(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{serviceId}")
    public ApiResponse<DataServiceDefinitionDtos.DetailResponse> update(
            @PathVariable UUID serviceId,
            @Valid @RequestBody DataServiceDefinitionDtos.UpdateRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toTarget(applicationService.update(body.toCommand(serviceId))),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{serviceId}/activate")
    public ApiResponse<DataServiceDefinitionDtos.DetailResponse> activate(
            @PathVariable UUID serviceId,
            @RequestHeader(name = "X-Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toTarget(applicationService.activate(serviceId, idempotencyKey)),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{serviceId}/disable")
    public ApiResponse<DataServiceDefinitionDtos.DetailResponse> disable(
            @PathVariable UUID serviceId,
            @RequestHeader(name = "X-Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toTarget(applicationService.disable(serviceId, idempotencyKey)),
                responseMetaFactory.create(request)
        );
    }

    @DeleteMapping("/{serviceId}")
    public ApiResponse<Void> delete(
            @PathVariable UUID serviceId,
            HttpServletRequest request
    ) {
        applicationService.delete(serviceId);
        return ApiResponse.success(null, responseMetaFactory.create(request));
    }

    @GetMapping("/{serviceId}/parameters")
    public ApiResponse<List<DataServiceDefinitionDtos.ParameterResponse>> listParameters(
            @PathVariable UUID serviceId,
            @RequestParam(required = false) Boolean required,
            @RequestParam(required = false) ServiceParameterDefinition.ParameterType paramType,
            @RequestParam(required = false) Boolean enabled,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.listParameters(serviceId, required, paramType, enabled).stream()
                        .map(dtoMapper::toParameterResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{serviceId}/parameters/{paramCode}")
    public ApiResponse<DataServiceDefinitionDtos.ParameterResponse> upsertParameter(
            @PathVariable UUID serviceId,
            @PathVariable String paramCode,
            @Valid @RequestBody DataServiceDefinitionDtos.UpsertParameterRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toParameterResponse(applicationService.upsertParameter(serviceId, body.toCommand(paramCode))),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/{serviceId}/field-mappings")
    public ApiResponse<List<DataServiceDefinitionDtos.FieldMappingResponse>> listFieldMappings(
            @PathVariable UUID serviceId,
            @RequestParam(required = false) String sourceField,
            @RequestParam(required = false) String targetField,
            @RequestParam(required = false) Boolean masked,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.listFieldMappings(serviceId, sourceField, targetField, masked).stream()
                        .map(dtoMapper::toFieldMappingResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{serviceId}/field-mappings/{mappingId}")
    public ApiResponse<DataServiceDefinitionDtos.FieldMappingResponse> upsertFieldMapping(
            @PathVariable UUID serviceId,
            @PathVariable UUID mappingId,
            @Valid @RequestBody DataServiceDefinitionDtos.UpsertFieldMappingRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toFieldMappingResponse(applicationService.upsertFieldMapping(serviceId, body.toCommand(mappingId))),
                responseMetaFactory.create(request)
        );
    }
}
