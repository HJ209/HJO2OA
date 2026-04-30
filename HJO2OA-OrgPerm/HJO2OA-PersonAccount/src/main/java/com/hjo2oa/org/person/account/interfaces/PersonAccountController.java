package com.hjo2oa.org.person.account.interfaces;

import com.hjo2oa.org.person.account.application.PersonAccountApplicationService;
import com.hjo2oa.org.person.account.domain.AccountView;
import com.hjo2oa.org.person.account.domain.PersonAccountView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.tenant.TenantContextHolder;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.ArrayList;
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
        UUID tenantId = requestTenantId(body.tenantId());
        return ApiResponse.success(
                dtoMapper.toPersonAccountResponse(applicationService.createPerson(body.toCommand(tenantId))),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/persons/{personId}")
    public ApiResponse<PersonAccountDtos.PersonAccountResponse> updatePerson(
            @PathVariable UUID personId,
            @Valid @RequestBody PersonAccountDtos.UpdatePersonRequest body,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(null);
        return ApiResponse.success(
                dtoMapper.toPersonAccountResponse(applicationService.updatePerson(body.toCommand(personId, tenantId))),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/persons/{personId}")
    public ApiResponse<PersonAccountDtos.PersonAccountResponse> getPerson(
            @PathVariable UUID personId,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(null);
        return ApiResponse.success(
                dtoMapper.toPersonAccountResponse(applicationService.getPerson(tenantId, personId)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/persons")
    public ApiResponse<List<PersonAccountDtos.PersonResponse>> listPersons(
            @RequestParam(required = false) UUID tenantId,
            HttpServletRequest request
    ) {
        UUID requestTenantId = requestTenantId(tenantId);
        return ApiResponse.success(
                applicationService.listPersons(requestTenantId).stream()
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
        UUID tenantId = requestTenantId(null);
        return ApiResponse.success(
                dtoMapper.toPersonAccountResponse(applicationService.disablePerson(tenantId, personId)),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/persons/{personId}/activate")
    public ApiResponse<PersonAccountDtos.PersonAccountResponse> activatePerson(
            @PathVariable UUID personId,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(null);
        return ApiResponse.success(
                dtoMapper.toPersonAccountResponse(applicationService.activatePerson(tenantId, personId)),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/persons/{personId}/resign")
    public ApiResponse<PersonAccountDtos.PersonAccountResponse> resignPerson(
            @PathVariable UUID personId,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(null);
        return ApiResponse.success(
                dtoMapper.toPersonAccountResponse(applicationService.resignPerson(tenantId, personId)),
                responseMetaFactory.create(request)
        );
    }

    @DeleteMapping("/persons/{personId}")
    public ApiResponse<Void> deletePerson(
            @PathVariable UUID personId,
            HttpServletRequest request
    ) {
        applicationService.deletePerson(requestTenantId(null), personId);
        return ApiResponse.success(null, responseMetaFactory.create(request));
    }

    @PostMapping("/persons/{personId}/accounts")
    public ApiResponse<PersonAccountDtos.PersonAccountResponse> createAccount(
            @PathVariable UUID personId,
            @Valid @RequestBody PersonAccountDtos.CreateAccountRequest body,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(null);
        return ApiResponse.success(
                dtoMapper.toPersonAccountResponse(applicationService.createAccount(body.toCommand(personId, tenantId))),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/accounts/{accountId}/credential")
    public ApiResponse<PersonAccountDtos.AccountResponse> updateAccountCredential(
            @PathVariable UUID accountId,
            @Valid @RequestBody PersonAccountDtos.UpdateAccountCredentialRequest body,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(null);
        return ApiResponse.success(
                dtoMapper.toAccountResponse(applicationService.updateAccountCredential(body.toCommand(accountId, tenantId))),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/accounts/{accountId}/lock")
    public ApiResponse<PersonAccountDtos.AccountResponse> lockAccount(
            @PathVariable UUID accountId,
            @Valid @RequestBody PersonAccountDtos.LockAccountRequest body,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(null);
        return ApiResponse.success(
                dtoMapper.toAccountResponse(applicationService.lockAccount(body.toCommand(accountId, tenantId))),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/accounts/{accountId}/unlock")
    public ApiResponse<PersonAccountDtos.AccountResponse> unlockAccount(
            @PathVariable UUID accountId,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(null);
        return ApiResponse.success(
                dtoMapper.toAccountResponse(applicationService.unlockAccount(tenantId, accountId)),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/accounts/{accountId}/disable")
    public ApiResponse<PersonAccountDtos.AccountResponse> disableAccount(
            @PathVariable UUID accountId,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(null);
        return ApiResponse.success(
                dtoMapper.toAccountResponse(applicationService.disableAccount(tenantId, accountId)),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/accounts/{accountId}/activate")
    public ApiResponse<PersonAccountDtos.AccountResponse> activateAccount(
            @PathVariable UUID accountId,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(null);
        return ApiResponse.success(
                dtoMapper.toAccountResponse(applicationService.activateAccount(tenantId, accountId)),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/persons/{personId}/accounts/{accountId}/primary")
    public ApiResponse<PersonAccountDtos.PersonAccountResponse> setPrimaryAccount(
            @PathVariable UUID personId,
            @PathVariable UUID accountId,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(null);
        return ApiResponse.success(
                dtoMapper.toPersonAccountResponse(applicationService.setPrimaryAccount(tenantId, personId, accountId)),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/accounts/{accountId}/login-record")
    public ApiResponse<PersonAccountDtos.AccountResponse> recordLogin(
            @PathVariable UUID accountId,
            @Valid @RequestBody PersonAccountDtos.LoginRecordRequest body,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(null);
        return ApiResponse.success(
                dtoMapper.toAccountResponse(applicationService.recordLogin(body.toCommand(accountId, tenantId))),
                responseMetaFactory.create(request)
        );
    }

    @DeleteMapping("/accounts/{accountId}")
    public ApiResponse<Void> deleteAccount(
            @PathVariable UUID accountId,
            HttpServletRequest request
    ) {
        applicationService.deleteAccount(requestTenantId(null), accountId);
        return ApiResponse.success(null, responseMetaFactory.create(request));
    }

    @GetMapping("/export")
    public ApiResponse<PersonAccountDtos.PersonAccountExportResponse> exportPersonAccounts(HttpServletRequest request) {
        UUID tenantId = requestTenantId(null);
        List<PersonAccountDtos.PersonResponse> persons = applicationService.listPersons(tenantId).stream()
                .map(dtoMapper::toPersonResponse)
                .toList();
        List<PersonAccountDtos.PersonAccountResponse> personAccounts = persons.stream()
                .map(person -> applicationService.getPerson(tenantId, person.id()))
                .map(dtoMapper::toPersonAccountResponse)
                .toList();
        return ApiResponse.success(
                new PersonAccountDtos.PersonAccountExportResponse(persons, personAccounts),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/import")
    public ApiResponse<PersonAccountDtos.PersonAccountImportResponse> importPersonAccounts(
            @Valid @RequestBody PersonAccountDtos.PersonAccountImportRequest body,
            HttpServletRequest request
    ) {
        UUID tenantId = requestTenantId(null);
        List<UUID> personIds = new ArrayList<>();
        List<UUID> accountIds = new ArrayList<>();
        if (body.persons() != null) {
            for (PersonAccountDtos.CreatePersonRequest item : body.persons()) {
                PersonAccountView view = applicationService.createPerson(item.toCommand(requestTenantId(item.tenantId())));
                if (!view.person().tenantId().equals(tenantId)) {
                    throw new BizException(SharedErrorDescriptors.BAD_REQUEST, "Import person tenant mismatch");
                }
                personIds.add(view.person().id());
            }
        }
        if (body.accounts() != null) {
            for (PersonAccountDtos.AccountImportItem item : body.accounts()) {
                PersonAccountView view = applicationService.createAccount(item.account().toCommand(item.personId(), tenantId));
                view.accounts().stream()
                        .filter(account -> account.personId().equals(item.personId()))
                        .filter(account -> account.username().equals(item.account().username()))
                        .map(AccountView::id)
                        .findFirst()
                        .ifPresent(accountIds::add);
            }
        }
        return ApiResponse.success(
                new PersonAccountDtos.PersonAccountImportResponse(
                        personIds.size(),
                        accountIds.size(),
                        personIds,
                        accountIds
                ),
                responseMetaFactory.create(request)
        );
    }

    private UUID requestTenantId(UUID requestValue) {
        UUID headerTenantId = TenantContextHolder.currentTenantId()
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.BAD_REQUEST, "X-Tenant-Id is required"));
        if (requestValue != null && !requestValue.equals(headerTenantId)) {
            throw new BizException(SharedErrorDescriptors.BAD_REQUEST, "Tenant id does not match X-Tenant-Id");
        }
        return headerTenantId;
    }
}
