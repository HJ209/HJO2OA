package com.hjo2oa.org.data.permission.interfaces;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.org.data.permission.application.DataPermissionApplicationService;
import com.hjo2oa.org.data.permission.application.DataPermissionCommands;
import com.hjo2oa.org.data.permission.domain.FieldPermissionAction;
import com.hjo2oa.org.data.permission.domain.FieldPermissionRuntimeMasker;
import com.hjo2oa.org.data.permission.domain.PermissionEffect;
import com.hjo2oa.org.data.permission.domain.PermissionSubjectType;
import com.hjo2oa.org.data.permission.domain.SubjectReference;
import com.hjo2oa.org.data.permission.infrastructure.DataPermissionRuntimeContext;
import com.hjo2oa.org.data.permission.infrastructure.InMemoryDataPermissionRepository;
import com.hjo2oa.shared.web.ApiResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DataPermissionFieldMaskingResponseBodyAdviceTest {

    @AfterEach
    void clearContext() {
        DataPermissionRuntimeContext.clear();
    }

    @Test
    void shouldMaskApiResponseMapWhenRuntimeContextIsPresent() {
        UUID roleId = UUID.randomUUID();
        DataPermissionApplicationService service = new DataPermissionApplicationService(
                new InMemoryDataPermissionRepository(),
                Clock.fixed(Instant.parse("2026-04-29T00:00:00Z"), ZoneOffset.UTC)
        );
        service.createFieldPolicy(new DataPermissionCommands.SaveFieldPolicyCommand(
                PermissionSubjectType.ROLE,
                roleId,
                "person_profile",
                "view",
                "mobile",
                FieldPermissionAction.DESENSITIZED,
                PermissionEffect.ALLOW,
                null
        ));
        DataPermissionRuntimeContext.set(
                "person_profile",
                null,
                java.util.List.of(new SubjectReference(PermissionSubjectType.ROLE, roleId))
        );
        DataPermissionFieldMaskingResponseBodyAdvice advice = new DataPermissionFieldMaskingResponseBodyAdvice(
                service,
                new FieldPermissionRuntimeMasker(),
                true
        );

        Object result = advice.beforeBodyWrite(
                ApiResponse.success(Map.of("name", "Alice", "mobile", "13812345678"), null),
                null,
                null,
                null,
                null,
                null
        );

        assertThat(result).isInstanceOf(ApiResponse.class);
        Object data = ((ApiResponse<?>) result).data();
        assertThat(data).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> masked = (Map<String, Object>) data;
        assertThat(masked)
                .containsEntry("name", "Alice")
                .containsEntry("mobile", "138****78");
    }
}
