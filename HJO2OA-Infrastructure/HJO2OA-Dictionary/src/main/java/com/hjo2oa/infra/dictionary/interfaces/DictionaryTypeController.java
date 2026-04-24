package com.hjo2oa.infra.dictionary.interfaces;

import com.hjo2oa.infra.dictionary.application.DictionaryTypeApplicationService;
import com.hjo2oa.infra.dictionary.domain.DictionaryTypeView;
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
@RequestMapping("/api/v1/infra/dictionaries")
public class DictionaryTypeController {

    private final DictionaryTypeApplicationService applicationService;
    private final DictionaryTypeDtoMapper dtoMapper;
    private final ResponseMetaFactory responseMetaFactory;

    public DictionaryTypeController(
            DictionaryTypeApplicationService applicationService,
            DictionaryTypeDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.dtoMapper = dtoMapper;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PostMapping
    public ApiResponse<DictionaryTypeDtos.DictionaryTypeResponse> create(
            @Valid @RequestBody DictionaryTypeDtos.CreateTypeRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.createType(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{typeId}/disable")
    public ApiResponse<DictionaryTypeDtos.DictionaryTypeResponse> disable(
            @PathVariable UUID typeId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.disableType(typeId)),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{typeId}/enable")
    public ApiResponse<DictionaryTypeDtos.DictionaryTypeResponse> enable(
            @PathVariable UUID typeId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResponse(applicationService.enableType(typeId)),
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
                dtoMapper.toResponse(applicationService.addItem(typeId, body.toCommand())),
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
                dtoMapper.toResponse(applicationService.updateItem(typeId, itemId, body.toCommand())),
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
                dtoMapper.toResponse(applicationService.removeItem(typeId, itemId)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/code/{code}")
    public ApiResponse<DictionaryTypeDtos.DictionaryTypeResponse> queryByCode(
            @PathVariable String code,
            @RequestParam(required = false) UUID tenantId,
            HttpServletRequest request
    ) {
        DictionaryTypeView dictionaryType = applicationService.queryByCode(tenantId, code)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Dictionary type not found"
                ));
        return ApiResponse.success(dtoMapper.toResponse(dictionaryType), responseMetaFactory.create(request));
    }

    @GetMapping
    public ApiResponse<List<DictionaryTypeDtos.DictionaryTypeResponse>> list(
            @RequestParam(required = false) UUID tenantId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.listTypes(tenantId).stream().map(dtoMapper::toResponse).toList(),
                responseMetaFactory.create(request)
        );
    }
}
