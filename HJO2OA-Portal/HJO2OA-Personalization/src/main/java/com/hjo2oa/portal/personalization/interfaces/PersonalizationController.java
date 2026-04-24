package com.hjo2oa.portal.personalization.interfaces;

import com.hjo2oa.portal.personalization.application.PersonalizationProfileApplicationService;
import com.hjo2oa.portal.personalization.domain.PersonalizationProfileView;
import com.hjo2oa.portal.personalization.domain.PersonalizationSceneType;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/portal/personalization")
public class PersonalizationController {

    private final PersonalizationProfileApplicationService applicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public PersonalizationController(
            PersonalizationProfileApplicationService applicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @GetMapping("/profile")
    public ApiResponse<PersonalizationProfileView> current(
            @RequestParam(name = "sceneType", defaultValue = "HOME") PersonalizationSceneType sceneType,
            HttpServletRequest request
    ) {
        return ApiResponse.success(applicationService.current(sceneType), responseMetaFactory.create(request));
    }

    @PostMapping("/profile")
    public ApiResponse<PersonalizationProfileView> save(
            @Valid @RequestBody SavePersonalizationProfileRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(applicationService.save(body.toCommand()), responseMetaFactory.create(request));
    }

    @PostMapping("/profile/reset")
    public ApiResponse<PersonalizationProfileView> reset(
            @Valid @RequestBody ResetPersonalizationProfileRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(applicationService.reset(body.toCommand()), responseMetaFactory.create(request));
    }
}
