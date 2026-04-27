package com.hjo2oa.wf.process.definition.interfaces;

import com.hjo2oa.wf.process.definition.domain.ActionDefinitionView;
import com.hjo2oa.wf.process.definition.domain.ProcessDefinitionView;
import org.springframework.stereotype.Component;

@Component
public class ProcessDefinitionDtoMapper {

    public ProcessDefinitionDtos.DefinitionResponse toDefinitionResponse(ProcessDefinitionView view) {
        return new ProcessDefinitionDtos.DefinitionResponse(
                view.id(),
                view.code(),
                view.name(),
                view.category(),
                view.version(),
                view.status(),
                view.formMetadataId(),
                view.startNodeId(),
                view.endNodeId(),
                view.nodes(),
                view.routes(),
                view.tenantId(),
                view.publishedAt(),
                view.publishedBy(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    public ProcessDefinitionDtos.ActionResponse toActionResponse(ActionDefinitionView view) {
        return new ProcessDefinitionDtos.ActionResponse(
                view.id(),
                view.code(),
                view.name(),
                view.category(),
                view.routeTarget(),
                view.requireOpinion(),
                view.requireTarget(),
                view.uiConfig(),
                view.tenantId(),
                view.createdAt(),
                view.updatedAt()
        );
    }
}
