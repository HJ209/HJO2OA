package com.hjo2oa.data.service.interfaces;

import com.hjo2oa.data.common.application.assembler.BaseAssembler;
import com.hjo2oa.data.service.domain.DataServiceDefinition;
import com.hjo2oa.data.service.domain.DataServiceViews;
import com.hjo2oa.data.service.domain.ServiceFieldMapping;
import com.hjo2oa.data.service.domain.ServiceParameterDefinition;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DataServiceDefinitionDtoMapper
        implements BaseAssembler<DataServiceViews.DetailView, DataServiceDefinitionDtos.DetailResponse> {

    public DataServiceDefinitionDtos.SummaryResponse toSummaryResponse(DataServiceViews.SummaryView view) {
        return new DataServiceDefinitionDtos.SummaryResponse(
                view.serviceId(),
                view.tenantId(),
                view.code(),
                view.name(),
                view.serviceType(),
                view.sourceMode(),
                view.permissionMode(),
                view.status(),
                view.cacheEnabled(),
                view.activatedAt(),
                view.openApiReferenceCount(),
                view.reportReferenceCount(),
                view.syncReferenceCount(),
                view.openApiReusable(),
                view.reportReusable(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    @Override
    public DataServiceDefinitionDtos.DetailResponse toTarget(DataServiceViews.DetailView view) {
        return new DataServiceDefinitionDtos.DetailResponse(
                view.serviceId(),
                view.tenantId(),
                view.code(),
                view.name(),
                view.serviceType(),
                view.sourceMode(),
                view.permissionMode(),
                toPermissionBoundaryResponse(view.permissionBoundary()),
                toCachePolicyResponse(view.cachePolicy()),
                view.status(),
                view.sourceRef(),
                view.connectorId(),
                view.description(),
                view.statusSequence(),
                view.activatedAt(),
                view.createdBy(),
                view.updatedBy(),
                view.createdAt(),
                view.updatedAt(),
                view.openApiReusable(),
                view.reportReusable(),
                view.parameters().stream().map(this::toParameterResponse).toList(),
                view.fieldMappings().stream().map(this::toFieldMappingResponse).toList()
        );
    }

    public DataServiceDefinitionDtos.ParameterResponse toParameterResponse(DataServiceViews.ParameterView view) {
        return new DataServiceDefinitionDtos.ParameterResponse(
                view.parameterId(),
                view.paramCode(),
                view.paramType(),
                view.required(),
                view.defaultValue(),
                toValidationRuleResponse(view.validationRule()),
                view.enabled(),
                view.description(),
                view.sortOrder()
        );
    }

    public DataServiceDefinitionDtos.FieldMappingResponse toFieldMappingResponse(DataServiceViews.FieldMappingView view) {
        return new DataServiceDefinitionDtos.FieldMappingResponse(
                view.mappingId(),
                view.sourceField(),
                view.targetField(),
                toTransformRuleResponse(view.transformRule()),
                view.masked(),
                view.description(),
                view.sortOrder()
        );
    }

    public DataServiceInvocationDtos.ExecutionPlanResponse toExecutionPlanResponse(DataServiceViews.ExecutionPlan plan) {
        return new DataServiceInvocationDtos.ExecutionPlanResponse(
                plan.serviceId(),
                plan.serviceCode(),
                plan.serviceType(),
                plan.sourceMode(),
                plan.permissionMode(),
                plan.cacheEnabled(),
                plan.cacheKey(),
                plan.cacheTtlSeconds(),
                plan.appCode(),
                plan.subjectId(),
                plan.idempotencyKey(),
                Map.copyOf(plan.normalizedParameters()),
                plan.outputMappings().stream().map(this::toFieldMappingResponse).toList(),
                List.copyOf(plan.outputFields()),
                plan.openApiReusable(),
                plan.reportReusable(),
                plan.preparedAt()
        );
    }

    private DataServiceDefinitionDtos.PermissionBoundaryResponse toPermissionBoundaryResponse(
            DataServiceDefinition.PermissionBoundary boundary
    ) {
        return new DataServiceDefinitionDtos.PermissionBoundaryResponse(
                boundary.allowedAppCodes(),
                boundary.allowedSubjectIds(),
                boundary.requiredRoles()
        );
    }

    private DataServiceDefinitionDtos.CachePolicyResponse toCachePolicyResponse(
            DataServiceDefinition.CachePolicy cachePolicy
    ) {
        return new DataServiceDefinitionDtos.CachePolicyResponse(
                cachePolicy.enabled(),
                cachePolicy.ttlSeconds(),
                cachePolicy.cacheKeyTemplate(),
                cachePolicy.scope(),
                cachePolicy.cacheNullValue(),
                cachePolicy.invalidationEvents()
        );
    }

    private DataServiceDefinitionDtos.ValidationRuleResponse toValidationRuleResponse(
            ServiceParameterDefinition.ValidationRule validationRule
    ) {
        if (validationRule == null) {
            return null;
        }
        return new DataServiceDefinitionDtos.ValidationRuleResponse(
                validationRule.minLength(),
                validationRule.maxLength(),
                validationRule.minValue(),
                validationRule.maxValue(),
                validationRule.regex(),
                validationRule.allowedValues(),
                validationRule.maxPageSize()
        );
    }

    private DataServiceDefinitionDtos.TransformRuleResponse toTransformRuleResponse(
            ServiceFieldMapping.TransformRule transformRule
    ) {
        if (transformRule == null) {
            return null;
        }
        return new DataServiceDefinitionDtos.TransformRuleResponse(
                transformRule.type(),
                transformRule.expression(),
                transformRule.formatPattern(),
                transformRule.constantValue()
        );
    }
}
