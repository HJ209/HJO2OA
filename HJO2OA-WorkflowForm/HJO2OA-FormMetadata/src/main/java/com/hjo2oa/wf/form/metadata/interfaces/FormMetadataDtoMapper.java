package com.hjo2oa.wf.form.metadata.interfaces;

import com.hjo2oa.wf.form.metadata.domain.FormMetadataDetailView;
import com.hjo2oa.wf.form.metadata.domain.FormMetadataView;
import com.hjo2oa.wf.form.metadata.domain.FormRenderSchemaView;
import org.springframework.stereotype.Component;

@Component
public class FormMetadataDtoMapper {

    public FormMetadataDtos.FormMetadataResponse toResponse(FormMetadataView view) {
        return new FormMetadataDtos.FormMetadataResponse(
                view.id(),
                view.code(),
                view.name(),
                view.nameI18nKey(),
                view.version(),
                view.status(),
                view.tenantId(),
                view.publishedAt(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    public FormMetadataDtos.FormMetadataDetailResponse toDetailResponse(FormMetadataDetailView view) {
        return new FormMetadataDtos.FormMetadataDetailResponse(
                view.id(),
                view.code(),
                view.name(),
                view.nameI18nKey(),
                view.version(),
                view.status(),
                view.fieldSchema(),
                view.layout(),
                view.validations(),
                view.fieldPermissionMap(),
                view.tenantId(),
                view.publishedAt(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    public FormMetadataDtos.RenderSchemaResponse toRenderSchemaResponse(FormRenderSchemaView view) {
        return new FormMetadataDtos.RenderSchemaResponse(
                view.metadataId(),
                view.code(),
                view.name(),
                view.nameI18nKey(),
                view.version(),
                view.fieldSchema(),
                view.layout(),
                view.validations(),
                view.fieldPermissionMap(),
                view.tenantId(),
                view.publishedAt()
        );
    }
}
