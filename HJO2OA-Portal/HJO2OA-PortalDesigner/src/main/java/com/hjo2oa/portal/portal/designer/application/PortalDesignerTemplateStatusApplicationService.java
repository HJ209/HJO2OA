package com.hjo2oa.portal.portal.designer.application;

import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateProjection;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateProjectionRepository;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateStatusView;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class PortalDesignerTemplateStatusApplicationService {

    private final PortalDesignerTemplateProjectionRepository projectionRepository;

    public PortalDesignerTemplateStatusApplicationService(
            PortalDesignerTemplateProjectionRepository projectionRepository
    ) {
        this.projectionRepository = Objects.requireNonNull(
                projectionRepository,
                "projectionRepository must not be null"
        );
    }

    public Optional<PortalDesignerTemplateStatusView> current(String templateId) {
        Objects.requireNonNull(templateId, "templateId must not be null");
        return projectionRepository.findByTemplateId(templateId).map(PortalDesignerTemplateProjection::toView);
    }
}
