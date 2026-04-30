package com.hjo2oa.wf.form.renderer.interfaces;

import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import com.hjo2oa.wf.form.renderer.application.FormSubmissionApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Objects;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping({"/api/v1/form/renderer/submissions", "/api/v1/wf/form-renderer/submissions"})
public class FormSubmissionController {

    private static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";

    private final FormSubmissionApplicationService applicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public FormSubmissionController(
            FormSubmissionApplicationService applicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = Objects.requireNonNull(applicationService, "applicationService must not be null");
        this.responseMetaFactory = Objects.requireNonNull(responseMetaFactory, "responseMetaFactory must not be null");
    }

    @PostMapping("/drafts")
    public ApiResponse<FormRendererDtos.FormSubmissionResponse> createDraft(
            @Valid @RequestBody FormRendererDtos.CreateDraftSubmissionRequest body,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                FormRendererDtos.FormSubmissionResponse.from(
                        applicationService.createDraft(body.toCommand(requireIdempotencyKey(idempotencyKey)))
                ),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/drafts/{submissionId}")
    public ApiResponse<FormRendererDtos.FormSubmissionResponse> updateDraft(
            @PathVariable UUID submissionId,
            @Valid @RequestBody FormRendererDtos.UpdateDraftSubmissionRequest body,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                FormRendererDtos.FormSubmissionResponse.from(
                        applicationService.updateDraft(body.toCommand(
                                submissionId,
                                requireIdempotencyKey(idempotencyKey)
                        ))
                ),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/drafts/{submissionId}/submit")
    public ApiResponse<FormRendererDtos.FormSubmissionResponse> submitDraft(
            @PathVariable UUID submissionId,
            @Valid @RequestBody FormRendererDtos.SubmitDraftSubmissionRequest body,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                FormRendererDtos.FormSubmissionResponse.from(
                        applicationService.submitDraft(body.toCommand(
                                submissionId,
                                requireIdempotencyKey(idempotencyKey)
                        ))
                ),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/{submissionId}")
    public ApiResponse<FormRendererDtos.FormSubmissionResponse> get(
            @PathVariable UUID submissionId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                FormRendererDtos.FormSubmissionResponse.from(applicationService.get(submissionId)),
                responseMetaFactory.create(request)
        );
    }

    private String requireIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BizException(SharedErrorDescriptors.BAD_REQUEST, "X-Idempotency-Key is required");
        }
        return idempotencyKey.trim();
    }
}
