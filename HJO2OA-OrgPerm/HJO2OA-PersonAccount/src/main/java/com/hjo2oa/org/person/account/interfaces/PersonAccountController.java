package com.hjo2oa.org.person.account.interfaces;

import com.hjo2oa.org.person.account.application.PersonAccountApplicationService;
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
@RequestMapping("/api/v1/org/person-accounts")
public class PersonAccountController {

    private final PersonAccountApplicationService applicationService;
    private final PersonAccountDtoMapper dtoMapper;
    private final ResponseMetaFactory responseMetaFactory;

    public PersonAccountController(
            PersonAccountApplicationService applicationService,
            PersonAccountDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.dtoMapper = dtoMapper;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PostMapping("/persons")
    public ApiResponse<PersonAccountDtos.PersonAccountResponse> createPerson(
            @Valid @RequestBody PersonAccountDtos.CreatePersonRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toPersonAccountResponse(applicationService.createPerson(body.toCommand())),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/persons/{personId}")
    public ApiResponse<PersonAccountDtos.PersonAccountResponse> updatePerson(
            @PathVariable UUID personId,
            @Valid @RequestBody PersonAccountDtos.UpdatePersonRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toPersonAccountResponse(applicationService.updatePerson(body.toCommand(personId))),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/persons/{personId}")
    public ApiResponse<PersonAccountDtos.PersonAccountResponse> getPerson(
            @PathVariable UUID personId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toPersonAccountResponse(applicationService.getPerson(personId)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/persons")
    public ApiResponse<List<PersonAccountDtos.PersonResponse>> listPersons(
            @RequestParam UUID tenantId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                applicationService.listPersons(tenantId).stream()
                        .map(dtoMapper::toPersonResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/persons/{personId}/disable")
    public ApiResponse<PersonAccountDtos.PersonAccountResponse> disablePerson(
            @PathVariable UUID personId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toPersonAccountResponse(applicationService.disablePerson(personId)),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/persons/{personId}/resign")
    public ApiResponse<PersonAccountDtos.PersonAccountResponse> resignPerson(
            @PathVariable UUID personId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toPersonAccountResponse(applicationService.resignPerson(personId)),
                responseMetaFactory.create(request)
        );
    }

    @DeleteMapping("/persons/{personId}")
    public ApiResponse<Void> deletePerson(
            @PathVariable UUID personId,
            HttpServletRequest request
    ) {
        applicationService.deletePerson(personId);
        return ApiResponse.success(null, responseMetaFactory.create(request));
    }

    @PostMapping("/persons/{personId}/accounts")
    public ApiResponse<PersonAccountDtos.PersonAccountResponse> createAccount(
            @PathVariable UUID personId,
            @Valid @RequestBody PersonAccountDtos.CreateAccountRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toPersonAccountResponse(applicationService.createAccount(body.toCommand(personId))),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/accounts/{accountId}/credential")
    public ApiResponse<PersonAccountDtos.AccountResponse> updateAccountCredential(
            @PathVariable UUID accountId,
            @Valid @RequestBody PersonAccountDtos.UpdateAccountCredentialRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toAccountResponse(applicationService.updateAccountCredential(body.toCommand(accountId))),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/accounts/{accountId}/lock")
    public ApiResponse<PersonAccountDtos.AccountResponse> lockAccount(
            @PathVariable UUID accountId,
            @Valid @RequestBody PersonAccountDtos.LockAccountRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toAccountResponse(applicationService.lockAccount(body.toCommand(accountId))),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/accounts/{accountId}/unlock")
    public ApiResponse<PersonAccountDtos.AccountResponse> unlockAccount(
            @PathVariable UUID accountId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toAccountResponse(applicationService.unlockAccount(accountId)),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/persons/{personId}/accounts/{accountId}/primary")
    public ApiResponse<PersonAccountDtos.PersonAccountResponse> setPrimaryAccount(
            @PathVariable UUID personId,
            @PathVariable UUID accountId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toPersonAccountResponse(applicationService.setPrimaryAccount(personId, accountId)),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/accounts/{accountId}/login-record")
    public ApiResponse<PersonAccountDtos.AccountResponse> recordLogin(
            @PathVariable UUID accountId,
            @Valid @RequestBody PersonAccountDtos.LoginRecordRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toAccountResponse(applicationService.recordLogin(body.toCommand(accountId))),
                responseMetaFactory.create(request)
        );
    }

    @DeleteMapping("/accounts/{accountId}")
    public ApiResponse<Void> deleteAccount(
            @PathVariable UUID accountId,
            HttpServletRequest request
    ) {
        applicationService.deleteAccount(accountId);
        return ApiResponse.success(null, responseMetaFactory.create(request));
    }
}
