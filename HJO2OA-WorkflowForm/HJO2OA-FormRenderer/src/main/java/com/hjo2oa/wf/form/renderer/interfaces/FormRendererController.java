package com.hjo2oa.wf.form.renderer.interfaces;

import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import com.hjo2oa.wf.form.renderer.application.FormRendererApplicationService;
import com.hjo2oa.wf.form.renderer.domain.FormValidationResultView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/wf/form-renderer")
public class FormRendererController {

    private final FormRendererApplicationService applicationService;
    private final FormRendererDtoMapper dtoMapper;
    private final ResponseMetaFactory responseMetaFactory;

    public FormRendererController(
            FormRendererApplicationService applicationService,
            FormRendererDtoMapper dtoMapper,
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

    @PostMapping("/render")
    public ApiResponse<FormRendererDtos.RenderResponse> render(
            @Valid @RequestBody FormRendererDtos.RenderRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toRenderResponse(applicationService.renderForm(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/validate")
    public ApiResponse<FormValidationResultView> validate(
            @Valid @RequestBody FormRendererDtos.ValidateRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.validateForm(body.toCommand()),
                responseMetaFactory.create(request)
        );
    }
}
