package com.hjo2oa.infra.data.i18n.interfaces;

import com.hjo2oa.infra.data.i18n.application.TranslationEntryApplicationService;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/infra/translations")
public class TranslationEntryController {

    private final TranslationEntryApplicationService applicationService;
    private final TranslationEntryDtoMapper dtoMapper;
    private final ResponseMetaFactory responseMetaFactory;

    public TranslationEntryController(
            TranslationEntryApplicationService applicationService,
            TranslationEntryDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = Objects.requireNonNull(
                applicationService,
                "applicationService must not be null"
        );
        this.dtoMapper = Objects.requireNonNull(dtoMapper, "dtoMapper must not be null");
        this.responseMetaFactory = Objects.requireNonNull(
                responseMetaFactory,
                "responseMetaFactory must not be null"
        );
    }

    @PostMapping
    public ApiResponse<TranslationEntryDtos.EntryResponse> create(
            @Valid @RequestBody TranslationEntryDtos.CreateRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toEntryResponse(applicationService.createTranslation(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{entryId}")
    public ApiResponse<TranslationEntryDtos.EntryResponse> update(
            @PathVariable UUID entryId,
            @Valid @RequestBody TranslationEntryDtos.UpdateRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toEntryResponse(applicationService.updateTranslation(body.toCommand(entryId))),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{entryId}/review")
    public ApiResponse<TranslationEntryDtos.EntryResponse> review(
            @PathVariable UUID entryId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toEntryResponse(applicationService.reviewTranslation(entryId)),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/resolve")
    public ApiResponse<TranslationEntryDtos.ResolveResponse> resolve(
            @Valid @RequestBody TranslationEntryDtos.ResolveRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toResolveResponse(applicationService.resolveTranslation(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/batch")
    public ApiResponse<List<TranslationEntryDtos.EntryResponse>> batchSave(
            @Valid @RequestBody TranslationEntryDtos.BatchSaveRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toEntryResponses(applicationService.batchSaveTranslations(body.toCommands())),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public ApiResponse<List<TranslationEntryDtos.EntryResponse>> queryByEntity(
            @PathVariable String entityType,
            @PathVariable String entityId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toEntryResponses(applicationService.queryByEntity(entityType, entityId)),
                responseMetaFactory.create(request)
        );
    }
}
