package com.hjo2oa.org.identity.context.application;

public record SwitchIdentityContextCommand(
        String targetPositionId,
        String reason
) {
}
