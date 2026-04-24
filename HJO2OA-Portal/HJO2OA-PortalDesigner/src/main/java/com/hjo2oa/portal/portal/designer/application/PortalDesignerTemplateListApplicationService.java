package com.hjo2oa.portal.portal.designer.application;

import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateProjection;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateProjectionRepository;
import com.hjo2oa.portal.portal.designer.domain.PortalDesignerTemplateStatusView;
import com.hjo2oa.portal.portal.model.domain.PortalModelContext;
import com.hjo2oa.portal.portal.model.domain.PortalModelContextProvider;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PortalDesignerTemplateListApplicationService {

    private final PortalDesignerTemplateProjectionRepository projectionRepository;
    private final PortalModelContextProvider contextProvider;

    public PortalDesignerTemplateListApplicationService(
            PortalDesignerTemplateProjectionRepository projectionRepository,
            PortalModelContextProvider contextProvider
    ) {
        this.projectionRepository = Objects.requireNonNull(
                projectionRepository,
                "projectionRepository must not be null"
        );
        this.contextProvider = Objects.requireNonNull(contextProvider, "contextProvider must not be null");
    }

    public List<PortalDesignerTemplateStatusView> list(PortalPublicationSceneType sceneType) {
        PortalModelContext context = contextProvider.currentContext();
        return projectionRepository.findAllByTenant(context.tenantId()).stream()
                .filter(projection -> sceneType == null || projection.sceneType() == sceneType)
                .map(PortalDesignerTemplateProjection::toView)
                .sorted(Comparator.comparing(PortalDesignerTemplateStatusView::templateCode)
                        .thenComparing(PortalDesignerTemplateStatusView::templateId))
                .toList();
    }
}
