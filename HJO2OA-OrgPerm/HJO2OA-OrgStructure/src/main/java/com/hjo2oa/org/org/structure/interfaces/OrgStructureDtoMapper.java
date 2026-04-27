package com.hjo2oa.org.org.structure.interfaces;

import com.hjo2oa.org.org.structure.domain.DepartmentView;
import com.hjo2oa.org.org.structure.domain.OrganizationView;
import org.springframework.stereotype.Component;

@Component
public class OrgStructureDtoMapper {

    public OrgStructureDtos.OrganizationResponse toOrganizationResponse(OrganizationView view) {
        return new OrgStructureDtos.OrganizationResponse(
                view.id(),
                view.code(),
                view.name(),
                view.shortName(),
                view.type(),
                view.parentId(),
                view.level(),
                view.path(),
                view.sortOrder(),
                view.status(),
                view.tenantId(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    public OrgStructureDtos.DepartmentResponse toDepartmentResponse(DepartmentView view) {
        return new OrgStructureDtos.DepartmentResponse(
                view.id(),
                view.code(),
                view.name(),
                view.organizationId(),
                view.parentId(),
                view.level(),
                view.path(),
                view.managerId(),
                view.sortOrder(),
                view.status(),
                view.tenantId(),
                view.createdAt(),
                view.updatedAt()
        );
    }
}
