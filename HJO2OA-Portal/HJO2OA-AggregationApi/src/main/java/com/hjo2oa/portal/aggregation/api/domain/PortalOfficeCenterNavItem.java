package com.hjo2oa.portal.aggregation.api.domain;

public record PortalOfficeCenterNavItem(
        String code,
        String title,
        long badgeCount,
        String actionLink
) {
}
