package com.hjo2oa.portal.portal.home.interfaces;

import com.hjo2oa.portal.portal.home.application.PortalHomePageAssemblyApplicationService;
import com.hjo2oa.portal.portal.home.domain.PortalHomePageView;
import com.hjo2oa.portal.portal.home.domain.PortalHomeSceneType;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/portal/home")
public class PortalHomeController {

    private final PortalHomePageAssemblyApplicationService pageAssemblyApplicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public PortalHomeController(
            PortalHomePageAssemblyApplicationService pageAssemblyApplicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.pageAssemblyApplicationService = pageAssemblyApplicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @GetMapping("/page")
    public ApiResponse<PortalHomePageView> page(
            @RequestParam(name = "sceneType", defaultValue = "HOME") PortalHomeSceneType sceneType,
            HttpServletRequest request
    ) {
        PortalHomePageView pageView = pageAssemblyApplicationService.page(sceneType);
        return ApiResponse.success(pageView, responseMetaFactory.create(request));
    }
}
