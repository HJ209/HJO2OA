package com.hjo2oa.org.identity.context.interfaces;

import com.hjo2oa.infra.security.infrastructure.jwt.JwtClaims;
import com.hjo2oa.infra.security.infrastructure.jwt.JwtTokenProvider;
import com.hjo2oa.org.identity.context.application.IdentityContextAuthenticationApplicationService;
import com.hjo2oa.org.identity.context.domain.IdentityContextView;
import com.hjo2oa.org.identity.context.interfaces.dto.LoginRequest;
import com.hjo2oa.org.identity.context.interfaces.dto.LoginResponse;
import com.hjo2oa.org.identity.context.interfaces.dto.RefreshRequest;
import com.hjo2oa.org.person.account.application.PersonAccountApplicationService;
import com.hjo2oa.org.person.account.application.PersonAccountApplicationService.AuthenticatedAccount;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping({"/api/v1/auth", "/api/auth"})
public class AuthController {

    private final PersonAccountApplicationService personAccountApplicationService;
    private final IdentityContextAuthenticationApplicationService identityContextAuthenticationApplicationService;
    private final JwtTokenProvider jwtTokenProvider;
    private final ResponseMetaFactory responseMetaFactory;

    public AuthController(
            PersonAccountApplicationService personAccountApplicationService,
            IdentityContextAuthenticationApplicationService identityContextAuthenticationApplicationService,
            JwtTokenProvider jwtTokenProvider,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.personAccountApplicationService = personAccountApplicationService;
        this.identityContextAuthenticationApplicationService = identityContextAuthenticationApplicationService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        AuthenticatedAccount account = personAccountApplicationService.authenticate(
                request.username(),
                request.password(),
                clientIp(servletRequest)
        );
        IdentityContextView identityContext = identityContextAuthenticationApplicationService.establish(account);
        String token = jwtTokenProvider.generateToken(
                identityContext.personId(),
                account.username(),
                identityContext.roleIds(),
                identityContext.tenantId(),
                identityContext.accountId(),
                identityContext.currentAssignmentId(),
                identityContext.currentPositionId(),
                identityContext.currentOrganizationId(),
                identityContext.currentDepartmentId(),
                identityContext.permissionSnapshotVersion()
        );
        JwtClaims claims = jwtTokenProvider.extractClaims(token);
        return ApiResponse.success(
                new LoginResponse(token, "Bearer", claims.expiresAt()),
                responseMetaFactory.create(servletRequest)
        );
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(
            @Valid @RequestBody RefreshRequest request,
            HttpServletRequest servletRequest
    ) {
        String token = jwtTokenProvider.refreshToken(request.token());
        JwtClaims claims = jwtTokenProvider.extractClaims(token);
        return ApiResponse.success(
                new LoginResponse(token, "Bearer", claims.expiresAt()),
                responseMetaFactory.create(servletRequest)
        );
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest servletRequest) {
        SecurityContextHolder.clearContext();
        return ApiResponse.success(null, responseMetaFactory.create(servletRequest));
    }

    private static String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
