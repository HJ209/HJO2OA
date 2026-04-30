package com.hjo2oa.infra.dictionary.interfaces;

import com.hjo2oa.infra.dictionary.application.DictionaryTypeApplicationService;
import com.hjo2oa.infra.dictionary.application.DictionaryRuntimeService;
import com.hjo2oa.infra.dictionary.application.SystemEnumDictionaryService;
import com.hjo2oa.infra.dictionary.domain.DictionaryTypeView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/infra/dictionaries")
public class DictionaryTypeController {

    private final DictionaryTypeApplicationService applicationService;
    private final SystemEnumDictionaryService systemEnumDictionaryService;
    private final DictionaryRuntimeService dictionaryRuntimeService;
    private final DictionaryTypeDtoMapper dtoMapper;
    private final ResponseMetaFactory responseMetaFactory;

    @Autowired
    public DictionaryTypeController(
            DictionaryTypeApplicationService applicationService,
            SystemEnumDictionaryService systemEnumDictionaryService,
            DictionaryRuntimeService dictionaryRuntimeService,
            DictionaryTypeDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.systemEnumDictionaryService = systemEnumDictionaryService;
        this.dictionaryRuntimeService = dictionaryRuntimeService;
        this.dtoMapper = dtoMapper;
        this.responseMetaFactory = responseMetaFactory;
    }

    public DictionaryTypeController(
            DictionaryTypeApplicationService applicationService,
            SystemEnumDictionaryService systemEnumDictionaryService,
            DictionaryTypeDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this(applicationService, systemEnumDictionaryService, null, dtoMapper, responseMetaFactory);
    }

    @PostMapping
    public ApiResponse<DictionaryTypeDtos.DictionaryTypeResponse> create(
            @Valid @RequestBody DictionaryTypeDtos.CreateTypeRequest body,
            HttpServletRequest request
    ) {
        UUID tenantId = resolveTenantId(request, body.tenantId());
        DictionaryTypeDtos.CreateTypeRequest resolvedBody = new DictionaryTypeDtos.CreateTypeRequest(
                body.code(),
                body.name(),
                body.category(),
                body.hierarchical(),
                body.cacheable(),
                body.sortOrder(),
                tenantId
        );
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.createType(resolvedBody.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{typeId}")
    public ApiResponse<DictionaryTypeDtos.DictionaryTypeResponse> update(
            @PathVariable UUID typeId,
            @Valid @RequestBody DictionaryTypeDtos.UpdateTypeRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.updateType(typeId, resolveTenantId(request, null), body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{typeId}/disable")
    public ApiResponse<DictionaryTypeDtos.DictionaryTypeResponse> disable(
            @PathVariable UUID typeId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.disableType(typeId, resolveTenantId(request, null))),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{typeId}/enable")
    public ApiResponse<DictionaryTypeDtos.DictionaryTypeResponse> enable(
            @PathVariable UUID typeId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.enableType(typeId, resolveTenantId(request, null))),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/types/{typeId}/items")
    public ApiResponse<List<DictionaryTypeDtos.DictionaryItemResponse>> listItemsByTypeId(
            @PathVariable UUID typeId,
            HttpServletRequest request
    ) {
        DictionaryTypeView dictionaryType = applicationService.listTypes(resolveTenantId(request, null), true).stream()
                .filter(type -> type.id().equals(typeId))
                .findFirst()
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Dictionary type not found"
                ));
        return ApiResponse.success(
                dictionaryType.items().stream().map(dtoMapper::toResponse).toList(),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{typeId}/items")
    public ApiResponse<DictionaryTypeDtos.DictionaryTypeResponse> addItem(
            @PathVariable UUID typeId,
            @Valid @RequestBody DictionaryTypeDtos.AddItemRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.addItem(typeId, resolveTenantId(request, null), body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{typeId}/items/{itemId}")
    public ApiResponse<DictionaryTypeDtos.DictionaryTypeResponse> updateItem(
            @PathVariable UUID typeId,
            @PathVariable UUID itemId,
            @Valid @RequestBody DictionaryTypeDtos.UpdateItemRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.updateItem(typeId, itemId, resolveTenantId(request, null), body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{typeId}/items/{itemId}/disable")
    public ApiResponse<DictionaryTypeDtos.DictionaryTypeResponse> disableItem(
            @PathVariable UUID typeId,
            @PathVariable UUID itemId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.disableItem(typeId, itemId, resolveTenantId(request, null))),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{typeId}/items/{itemId}/enable")
    public ApiResponse<DictionaryTypeDtos.DictionaryTypeResponse> enableItem(
            @PathVariable UUID typeId,
            @PathVariable UUID itemId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.enableItem(typeId, itemId, resolveTenantId(request, null))),
                responseMetaFactory.create(request)
        );
    }

    @DeleteMapping("/{typeId}/items/{itemId}")
    public ApiResponse<DictionaryTypeDtos.DictionaryTypeResponse> removeItem(
            @PathVariable UUID typeId,
            @PathVariable UUID itemId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.removeItem(typeId, itemId, resolveTenantId(request, null))),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/code/{code}")
    public ApiResponse<DictionaryTypeDtos.DictionaryTypeResponse> queryByCode(
            @PathVariable String code,
            @RequestParam(required = false) UUID tenantId,
            HttpServletRequest request
    ) {
        DictionaryTypeView dictionaryType = applicationService.queryByCode(resolveTenantId(request, tenantId), code)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Dictionary type not found"
                ));
        return ApiResponse.success(dtoMapper.toResponse(dictionaryType), responseMetaFactory.create(request));
    }

    @GetMapping
    public ApiResponse<List<DictionaryTypeDtos.DictionaryTypeResponse>> list(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(defaultValue = "false") boolean includeDisabled,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.listTypes(resolveTenantId(request, tenantId), includeDisabled).stream()
                        .map(dtoMapper::toResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/{code}")
    public ApiResponse<DictionaryTypeDtos.RuntimeDictionaryResponse> queryRuntimeByCode(
            @PathVariable String code,
            @RequestParam(defaultValue = "true") boolean enabledOnly,
            @RequestParam(defaultValue = "false") boolean tree,
            @RequestParam(required = false) String language,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                toRuntimeDictionaryResponse(requireRuntimeService()
                        .query(resolveTenantId(request, null), code, enabledOnly, tree, resolveLanguage(request, language))),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/{code}/items")
    public ApiResponse<List<DictionaryTypeDtos.RuntimeItemResponse>> queryRuntimeItems(
            @PathVariable String code,
            @RequestParam(defaultValue = "true") boolean enabledOnly,
            @RequestParam(required = false) String language,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                toRuntimeDictionaryResponse(requireRuntimeService()
                        .query(resolveTenantId(request, null), code, enabledOnly, false, resolveLanguage(request, language)))
                        .items(),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/{code}/tree")
    public ApiResponse<List<DictionaryTypeDtos.RuntimeItemResponse>> queryRuntimeTree(
            @PathVariable String code,
            @RequestParam(defaultValue = "true") boolean enabledOnly,
            @RequestParam(required = false) String language,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                toRuntimeDictionaryResponse(requireRuntimeService()
                        .query(resolveTenantId(request, null), code, enabledOnly, true, resolveLanguage(request, language)))
                        .items(),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/batch")
    public ApiResponse<Map<String, DictionaryTypeDtos.RuntimeDictionaryResponse>> queryRuntimeBatch(
            @Valid @RequestBody DictionaryTypeDtos.BatchRuntimeRequest body,
            @RequestParam(required = false) String language,
            HttpServletRequest request
    ) {
        boolean enabledOnly = body.enabledOnly() == null || body.enabledOnly();
        boolean tree = Boolean.TRUE.equals(body.tree());
        Map<String, DictionaryTypeDtos.RuntimeDictionaryResponse> data = requireRuntimeService()
                .batch(resolveTenantId(request, null), body.codes(), enabledOnly, tree, resolveLanguage(request, language))
                .entrySet()
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> toRuntimeDictionaryResponse(entry.getValue()),
                        (left, right) -> left,
                        java.util.LinkedHashMap::new
                ));
        return ApiResponse.success(data, responseMetaFactory.create(request));
    }

    @PostMapping("/{code}/cache/refresh")
    public ApiResponse<DictionaryTypeDtos.RuntimeDictionaryResponse> refreshDictionaryCache(
            @PathVariable String code,
            @RequestParam(defaultValue = "true") boolean tree,
            @RequestParam(required = false) String language,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                toRuntimeDictionaryResponse(requireRuntimeService()
                        .refresh(resolveTenantId(request, null), code, tree, resolveLanguage(request, language))),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/system-enums")
    public ApiResponse<List<DictionaryTypeDtos.SystemEnumDictionaryResponse>> previewSystemEnums(
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                systemEnumDictionaryService.previewSystemEnums().stream()
                        .map(this::toSystemEnumDictionaryResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/system-enums/import")
    public ApiResponse<DictionaryTypeDtos.SystemEnumImportResponse> importSystemEnums(
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                toSystemEnumImportResponse(systemEnumDictionaryService.importSystemEnums()),
                responseMetaFactory.create(request)
        );
    }

    private DictionaryTypeDtos.SystemEnumDictionaryResponse toSystemEnumDictionaryResponse(
            SystemEnumDictionaryService.SystemEnumDictionaryView view
    ) {
        return new DictionaryTypeDtos.SystemEnumDictionaryResponse(
                view.code(),
                view.name(),
                view.className(),
                view.category(),
                view.imported(),
                view.newItemCodes(),
                view.changedItemCodes(),
                view.disabledItemCodes(),
                view.items().stream()
                        .map(item -> new DictionaryTypeDtos.SystemEnumItemResponse(
                                item.code(),
                                item.name(),
                                item.sortOrder()
                        ))
                        .toList()
        );
    }

    private DictionaryTypeDtos.SystemEnumImportResponse toSystemEnumImportResponse(
            SystemEnumDictionaryService.SystemEnumImportResult result
    ) {
        return new DictionaryTypeDtos.SystemEnumImportResponse(
                result.discoveredTypes(),
                result.createdTypes(),
                result.createdItems(),
                result.updatedItems(),
                result.disabledItems(),
                result.importedCodes()
        );
    }

    private DictionaryTypeDtos.RuntimeDictionaryResponse toRuntimeDictionaryResponse(
            DictionaryRuntimeService.RuntimeDictionaryView view
    ) {
        return new DictionaryTypeDtos.RuntimeDictionaryResponse(
                view.id(),
                view.code(),
                view.name(),
                view.category(),
                view.hierarchical(),
                view.tenantId(),
                view.language(),
                view.items().stream().map(this::toRuntimeItemResponse).toList()
        );
    }

    private DictionaryTypeDtos.RuntimeItemResponse toRuntimeItemResponse(
            DictionaryRuntimeService.RuntimeItemView view
    ) {
        return new DictionaryTypeDtos.RuntimeItemResponse(
                view.id(),
                view.code(),
                view.label(),
                view.value(),
                view.parentId(),
                view.sortOrder(),
                view.enabled(),
                view.defaultItem(),
                view.extensionJson(),
                view.children().stream().map(this::toRuntimeItemResponse).toList()
        );
    }

    private DictionaryRuntimeService requireRuntimeService() {
        if (dictionaryRuntimeService == null) {
            throw new BizException(SharedErrorDescriptors.INTERNAL_ERROR, "Dictionary runtime is unavailable");
        }
        return dictionaryRuntimeService;
    }

    private UUID resolveTenantId(HttpServletRequest request, UUID fallbackTenantId) {
        String headerValue = request == null ? null : request.getHeader("X-Tenant-Id");
        if (headerValue == null || headerValue.isBlank()) {
            return fallbackTenantId;
        }
        try {
            return UUID.fromString(headerValue);
        } catch (IllegalArgumentException ex) {
            throw new BizException(SharedErrorDescriptors.BAD_REQUEST, "Invalid X-Tenant-Id header", ex);
        }
    }

    private String resolveLanguage(HttpServletRequest request, String explicitLanguage) {
        if (explicitLanguage != null && !explicitLanguage.isBlank()) {
            return explicitLanguage;
        }
        String headerValue = request == null ? null : request.getHeader("Accept-Language");
        return headerValue == null || headerValue.isBlank() ? "zh-CN" : headerValue;
    }
}
