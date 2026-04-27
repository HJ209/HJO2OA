package com.hjo2oa.wf.form.renderer.interfaces;

import com.hjo2oa.wf.form.renderer.domain.RenderedFormView;
import org.springframework.stereotype.Component;

@Component
public class FormRendererDtoMapper {

    public FormRendererDtos.RenderResponse toRenderResponse(RenderedFormView view) {
        return new FormRendererDtos.RenderResponse(
                view.metadataId(),
                view.code(),
                view.name(),
                view.displayName(),
                view.version(),
                view.nodeId(),
                view.locale(),
                view.processInstanceId(),
                view.formDataId(),
                view.layout(),
                view.fields(),
                view.validation()
        );
    }
}
