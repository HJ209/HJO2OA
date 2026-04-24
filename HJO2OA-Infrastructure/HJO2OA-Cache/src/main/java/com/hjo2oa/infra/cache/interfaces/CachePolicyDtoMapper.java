package com.hjo2oa.infra.cache.interfaces;

import com.hjo2oa.infra.cache.domain.CacheInvalidationView;
import com.hjo2oa.infra.cache.domain.CachePolicyView;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CachePolicyDtoMapper {

    public CachePolicyDtos.PolicyResponse toPolicyResponse(CachePolicyView view) {
        return new CachePolicyDtos.PolicyResponse(
                view.id(),
                view.namespace(),
                view.backendType(),
                view.ttlSeconds(),
                view.maxCapacity(),
                view.evictionPolicy(),
                view.invalidationMode(),
                view.metricsEnabled(),
                view.active(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    public List<CachePolicyDtos.PolicyResponse> toPolicyResponses(List<CachePolicyView> views) {
        return views.stream().map(this::toPolicyResponse).toList();
    }

    public CachePolicyDtos.InvalidationResponse toInvalidationResponse(CacheInvalidationView view) {
        return new CachePolicyDtos.InvalidationResponse(
                view.id(),
                view.cachePolicyId(),
                view.namespace(),
                view.invalidateKey(),
                view.reasonType(),
                view.reasonRef(),
                view.invalidatedAt()
        );
    }
}
