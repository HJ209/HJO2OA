package com.hjo2oa.infra.i18n.interfaces;

import com.hjo2oa.infra.i18n.application.LocaleBundleApplicationService;
import com.hjo2oa.infra.i18n.domain.LocaleBundleView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.web.ApiResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/infra/i18n/bundles")
public class LocaleBundleController {

    private final LocaleBundleApplicationService applicationService;
    private final LocaleBundleDtoMapper dtoMapper;
    private final ResponseMetaFactory responseMetaFactory;

    public LocaleBundleController(
            LocaleBundleApplicationService applicationService,
            LocaleBundleDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.dtoMapper = dtoMapper;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PostMapping
    public ApiResponse<LocaleBundleDtos.BundleResponse> create(
            @Valid @RequestBody LocaleBundleDtos.CreateBundleRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toBundleResponse(applicationService.createBundle(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping
    public ApiResponse<List<LocaleBundleDtos.BundleResponse>> list(
            @RequestParam(required = false) String moduleCode,
            @RequestParam(required = false) String locale,
            @RequestParam(required = false) UUID tenantId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.listBundles(moduleCode, locale, tenantId).stream()
                        .map(dtoMapper::toBundleResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{bundleId}")
    public ApiResponse<LocaleBundleDtos.BundleResponse> update(
            @PathVariable UUID bundleId,
            @Valid @RequestBody LocaleBundleDtos.UpdateBundleRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toBundleResponse(applicationService.updateBundle(bundleId, body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{bundleId}/activate")
    public ApiResponse<LocaleBundleDtos.BundleResponse> activate(
            @PathVariable UUID bundleId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toBundleResponse(applicationService.activateBundle(bundleId)),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{bundleId}/deprecate")
    public ApiResponse<LocaleBundleDtos.BundleResponse> deprecate(
            @PathVariable UUID bundleId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toBundleResponse(applicationService.deprecateBundle(bundleId)),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{bundleId}/entries")
    public ApiResponse<LocaleBundleDtos.BundleResponse> addEntry(
            @PathVariable UUID bundleId,
            @Valid @RequestBody LocaleBundleDtos.EntryRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toBundleResponse(applicationService.addEntry(body.toCommand(bundleId))),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{bundleId}/entries/{key:.+}")
    public ApiResponse<LocaleBundleDtos.BundleResponse> updateEntry(
            @PathVariable UUID bundleId,
            @PathVariable("key") String key,
            @Valid @RequestBody LocaleBundleDtos.UpdateEntryRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toBundleResponse(applicationService.updateEntry(bundleId, key, body.resourceValue())),
                responseMetaFactory.create(request)
        );
    }

    @DeleteMapping("/{bundleId}/entries/{key:.+}")
    public ApiResponse<LocaleBundleDtos.BundleResponse> removeEntry(
            @PathVariable UUID bundleId,
            @PathVariable("key") String key,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toBundleResponse(applicationService.removeEntry(bundleId, key)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/code/{bundleCode}")
    public ApiResponse<List<LocaleBundleDtos.BundleResponse>> queryByCode(
            @PathVariable String bundleCode,
            HttpServletRequest request
    ) {
        List<LocaleBundleView> bundles = applicationService.queryByCode(bundleCode);
        if (bundles.isEmpty()) {
            throw new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Locale bundle not found");
        }
        return ApiResponse.success(
                bundles.stream().map(dtoMapper::toBundleResponse).toList(),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/locale/{locale}")
    public ApiResponse<List<LocaleBundleDtos.BundleResponse>> queryByLocale(
            @PathVariable String locale,
            @RequestParam(required = false) UUID tenantId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.queryByLocale(locale, tenantId).stream()
                        .map(dtoMapper::toBundleResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/resolve")
    public ApiResponse<LocaleBundleDtos.ResolvedMessageResponse> resolve(
            @Valid @RequestBody LocaleBundleDtos.ResolveMessageRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResolvedMessageResponse(applicationService.resolveMessage(body.toQuery())),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/module/{moduleCode}/locale/{locale}")
    public ApiResponse<List<LocaleBundleDtos.BundleResponse>> queryByModule(
            @PathVariable String moduleCode,
            @PathVariable String locale,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.queryByModule(moduleCode, locale).stream()
                        .map(dtoMapper::toBundleResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }
}
