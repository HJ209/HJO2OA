package com.hjo2oa.infra.security.interfaces;

import com.hjo2oa.infra.security.application.CryptoResult;
import com.hjo2oa.infra.security.application.CryptoService;
import com.hjo2oa.infra.security.application.MaskingService;
import com.hjo2oa.infra.security.application.PasswordPolicyService;
import com.hjo2oa.infra.security.application.PasswordValidationResult;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@UseSharedWebContract
@RequestMapping("/api/v1/infra/security")
public class SecurityRuntimeController {

    private final CryptoService cryptoService;
    private final MaskingService maskingService;
    private final PasswordPolicyService passwordPolicyService;
    private final ResponseMetaFactory responseMetaFactory;

    public SecurityRuntimeController(
            CryptoService cryptoService,
            MaskingService maskingService,
            PasswordPolicyService passwordPolicyService,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.cryptoService = cryptoService;
        this.maskingService = maskingService;
        this.passwordPolicyService = passwordPolicyService;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PostMapping("/crypto/encrypt")
    public ApiResponse<SecurityPolicyDtos.CryptoResponse> encrypt(
            @Valid @RequestBody SecurityPolicyDtos.CryptoRequest body,
            HttpServletRequest request
    ) {
        CryptoResult result = cryptoService.encrypt(body.keyRef(), body.algorithm(), body.value());
        return ApiResponse.success(
                new SecurityPolicyDtos.CryptoResponse(result.keyRef(), result.algorithm(), result.value()),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/crypto/decrypt")
    public ApiResponse<SecurityPolicyDtos.CryptoResponse> decrypt(
            @Valid @RequestBody SecurityPolicyDtos.CryptoRequest body,
            HttpServletRequest request
    ) {
        CryptoResult result = cryptoService.decrypt(body.keyRef(), body.algorithm(), body.value());
        return ApiResponse.success(
                new SecurityPolicyDtos.CryptoResponse(result.keyRef(), result.algorithm(), result.value()),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/keys/{keyRef}/rotate")
    public ApiResponse<SecurityPolicyDtos.KeyRotationResponse> rotateKeyRef(
            @PathVariable String keyRef,
            HttpServletRequest request
    ) {
        int version = cryptoService.rotate(keyRef);
        return ApiResponse.success(
                new SecurityPolicyDtos.KeyRotationResponse(keyRef, version),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/masking/preview")
    public ApiResponse<SecurityPolicyDtos.MaskValueResponse> previewMasking(
            @Valid @RequestBody SecurityPolicyDtos.MaskingPreviewRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                new SecurityPolicyDtos.MaskValueResponse(maskingService.mask(
                        body.policyCode(),
                        body.dataType(),
                        body.value()
                )),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/password/validate")
    public ApiResponse<SecurityPolicyDtos.PasswordValidationResponse> validatePassword(
            @Valid @RequestBody SecurityPolicyDtos.PasswordValidationRequest body,
            HttpServletRequest request
    ) {
        PasswordValidationResult result = passwordPolicyService.validate(
                body.policyCode(),
                body.password(),
                body.username(),
                body.passwordHistory()
        );
        return ApiResponse.success(
                new SecurityPolicyDtos.PasswordValidationResponse(result.accepted(), result.violations()),
                responseMetaFactory.create(request)
        );
    }
}
