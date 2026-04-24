package com.hjo2oa.infra.errorcode.interfaces;

import com.hjo2oa.infra.errorcode.application.ErrorCodeDefinitionCommands;
import com.hjo2oa.infra.errorcode.domain.ErrorCodeDefinitionView;
import org.springframework.stereotype.Component;

@Component
public class ErrorCodeDefinitionDtoMapper {

    public ErrorCodeDefinitionCommands.DefineCommand toDefineCommand(ErrorCodeDefinitionDtos.DefineRequest request) {
        return new ErrorCodeDefinitionCommands.DefineCommand(
                request.code(),
                request.moduleCode(),
                request.severity(),
                request.httpStatus(),
                request.messageKey(),
                request.category(),
                Boolean.TRUE.equals(request.retryable())
        );
    }

    public ErrorCodeDefinitionDtos.DetailResponse toDetailResponse(ErrorCodeDefinitionView view) {
        return new ErrorCodeDefinitionDtos.DetailResponse(
                view.id(),
                view.code(),
                view.moduleCode(),
                view.category(),
                view.severity(),
                view.httpStatus(),
                view.messageKey(),
                view.retryable(),
                view.deprecated(),
                view.createdAt(),
                view.updatedAt()
        );
    }
}
