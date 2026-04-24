package com.hjo2oa.infra.timezone.interfaces;

import com.hjo2oa.infra.timezone.domain.ResolvedTimezoneView;
import com.hjo2oa.infra.timezone.domain.TimezoneSettingView;
import java.time.Instant;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class TimezoneSettingDtoMapper {

    public TimezoneSettingDtos.TimezoneSettingResponse toResponse(TimezoneSettingView view) {
        return new TimezoneSettingDtos.TimezoneSettingResponse(
                view.id(),
                view.scopeType(),
                view.scopeId(),
                view.timezoneId(),
                view.isDefault(),
                view.effectiveFrom(),
                view.active(),
                view.tenantId(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    public TimezoneSettingDtos.ResolvedTimezoneResponse toResponse(ResolvedTimezoneView view) {
        return new TimezoneSettingDtos.ResolvedTimezoneResponse(
                view.settingId(),
                view.tenantId(),
                view.personId(),
                view.scopeType(),
                view.scopeId(),
                view.timezoneId(),
                view.isDefault(),
                view.effectiveFrom()
        );
    }

    public TimezoneSettingDtos.ConvertToUtcResponse toConvertToUtcResponse(Instant utcInstant, String timezoneId) {
        return new TimezoneSettingDtos.ConvertToUtcResponse(utcInstant, timezoneId);
    }

    public TimezoneSettingDtos.ConvertFromUtcResponse toConvertFromUtcResponse(
            LocalDateTime localDateTime,
            String timezoneId
    ) {
        return new TimezoneSettingDtos.ConvertFromUtcResponse(localDateTime, timezoneId);
    }
}
