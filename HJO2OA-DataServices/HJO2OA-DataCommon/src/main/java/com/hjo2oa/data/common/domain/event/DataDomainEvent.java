package com.hjo2oa.data.common.domain.event;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.util.Map;

public interface DataDomainEvent extends DomainEvent {

    String moduleCode();

    String aggregateCode();

    String operatorId();

    Map<String, Object> payload();
}
