package com.hjo2oa.data.openapi.interfaces;

import com.hjo2oa.data.openapi.application.OpenApiInvocationApplicationService;
import com.hjo2oa.data.openapi.application.OpenApiInvocationRequest;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Map;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
public class OpenApiPublicInvocationController {

    private final OpenApiInvocationApplicationService applicationService;
    private final ResponseMetaFactory responseMetaFactory;

    public OpenApiPublicInvocationController(
            OpenApiInvocationApplicationService applicationService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @RequestMapping("/api/open/**")
    public ApiResponse<Map<String, Object>> invoke(
            HttpServletRequest request,
            @RequestBody(required = false) String body
    ) {
        Map<String, String> queryParameters = request.getParameterMap().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> Arrays.stream(entry.getValue()).findFirst().orElse("")
                ));
        return ApiResponse.success(
                applicationService.invoke(new OpenApiInvocationRequest(queryParameters, body)),
                responseMetaFactory.create(request)
        );
    }
}
