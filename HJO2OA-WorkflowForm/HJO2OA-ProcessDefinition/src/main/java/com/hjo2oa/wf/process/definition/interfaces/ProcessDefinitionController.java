package com.hjo2oa.wf.process.definition.interfaces;

import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.UseSharedWebContract;
import com.hjo2oa.wf.process.definition.application.ProcessDefinitionApplicationService;
import com.hjo2oa.wf.process.definition.application.ProcessDefinitionCommands;
import com.hjo2oa.wf.process.definition.domain.ActionCategory;
import com.hjo2oa.wf.process.definition.domain.DefinitionStatus;
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
@RequestMapping("/api/v1/workflow/process-definitions")
public class ProcessDefinitionController {

    private final ProcessDefinitionApplicationService applicationService;
    private final ProcessDefinitionDtoMapper dtoMapper;
    private final ResponseMetaFactory responseMetaFactory;

    public ProcessDefinitionController(
            ProcessDefinitionApplicationService applicationService,
            ProcessDefinitionDtoMapper dtoMapper,
            ResponseMetaFactory responseMetaFactory
    ) {
        this.applicationService = applicationService;
        this.dtoMapper = dtoMapper;
        this.responseMetaFactory = responseMetaFactory;
    }

    @PostMapping
    public ApiResponse<ProcessDefinitionDtos.DefinitionResponse> createDefinition(
            @Valid @RequestBody ProcessDefinitionDtos.SaveDefinitionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDefinitionResponse(applicationService.createDefinition(body.toCommand(null))),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{definitionId}")
    public ApiResponse<ProcessDefinitionDtos.DefinitionResponse> updateDefinition(
            @PathVariable UUID definitionId,
            @Valid @RequestBody ProcessDefinitionDtos.SaveDefinitionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDefinitionResponse(applicationService.updateDefinition(body.toCommand(definitionId))),
                responseMetaFactory.create(request)
        );
    }

    @PostMapping("/{definitionId}/versions")
    public ApiResponse<ProcessDefinitionDtos.DefinitionResponse> createNextVersion(
            @PathVariable UUID definitionId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDefinitionResponse(applicationService.createNextVersion(definitionId)),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{definitionId}/publish")
    public ApiResponse<ProcessDefinitionDtos.DefinitionResponse> publishDefinition(
            @PathVariable UUID definitionId,
            @RequestBody(required = false) ProcessDefinitionDtos.PublishDefinitionRequest body,
            HttpServletRequest request
    ) {
        ProcessDefinitionDtos.PublishDefinitionRequest publishRequest =
                body == null ? new ProcessDefinitionDtos.PublishDefinitionRequest(null) : body;
        return ApiResponse.success(
                dtoMapper.toDefinitionResponse(applicationService.publishDefinition(publishRequest.toCommand(definitionId))),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/{definitionId}/deprecate")
    public ApiResponse<ProcessDefinitionDtos.DefinitionResponse> deprecateDefinition(
            @PathVariable UUID definitionId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDefinitionResponse(applicationService.deprecateDefinition(definitionId)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/{definitionId}")
    public ApiResponse<ProcessDefinitionDtos.DefinitionResponse> getDefinition(
            @PathVariable UUID definitionId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toDefinitionResponse(applicationService.getDefinition(definitionId)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping
    public ApiResponse<List<ProcessDefinitionDtos.DefinitionResponse>> queryDefinitions(
            @RequestParam UUID tenantId,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) DefinitionStatus status,
            HttpServletRequest request
    ) {
        ProcessDefinitionCommands.DefinitionQuery query =
                new ProcessDefinitionCommands.DefinitionQuery(tenantId, code, category, status);
        return ApiResponse.success(
                applicationService.queryDefinitions(query).stream()
                        .map(dtoMapper::toDefinitionResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @DeleteMapping("/{definitionId}")
    public ApiResponse<Void> deleteDefinition(
            @PathVariable UUID definitionId,
            HttpServletRequest request
    ) {
        applicationService.deleteDefinition(definitionId);
        return ApiResponse.success(null, responseMetaFactory.create(request));
    }

    @PostMapping("/actions")
    public ApiResponse<ProcessDefinitionDtos.ActionResponse> createAction(
            @Valid @RequestBody ProcessDefinitionDtos.SaveActionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toActionResponse(applicationService.createAction(body.toCommand(null))),
                responseMetaFactory.create(request)
        );
    }

    @PutMapping("/actions/{actionId}")
    public ApiResponse<ProcessDefinitionDtos.ActionResponse> updateAction(
            @PathVariable UUID actionId,
            @Valid @RequestBody ProcessDefinitionDtos.SaveActionRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toActionResponse(applicationService.updateAction(body.toCommand(actionId))),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/actions/{actionId}")
    public ApiResponse<ProcessDefinitionDtos.ActionResponse> getAction(
            @PathVariable UUID actionId,
            HttpServletRequest request
    ) {
        return ApiResponse.success(
                dtoMapper.toActionResponse(applicationService.getAction(actionId)),
                responseMetaFactory.create(request)
        );
    }

    @GetMapping("/actions")
    public ApiResponse<List<ProcessDefinitionDtos.ActionResponse>> queryActions(
            @RequestParam UUID tenantId,
            @RequestParam(required = false) ActionCategory category,
            HttpServletRequest request
    ) {
        ProcessDefinitionCommands.ActionQuery query = new ProcessDefinitionCommands.ActionQuery(tenantId, category);
        return ApiResponse.success(
                applicationService.queryActions(query).stream()
                        .map(dtoMapper::toActionResponse)
                        .toList(),
                responseMetaFactory.create(request)
        );
    }

    @DeleteMapping("/actions/{actionId}")
    public ApiResponse<Void> deleteAction(
            @PathVariable UUID actionId,
            HttpServletRequest request
    ) {
        applicationService.deleteAction(actionId);
        return ApiResponse.success(null, responseMetaFactory.create(request));
    }
}
