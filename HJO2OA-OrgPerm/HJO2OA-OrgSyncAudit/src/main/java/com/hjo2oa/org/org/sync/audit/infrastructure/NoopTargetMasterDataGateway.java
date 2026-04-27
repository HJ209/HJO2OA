package com.hjo2oa.org.org.sync.audit.infrastructure;

import com.hjo2oa.org.org.sync.audit.application.TargetMasterDataGateway;
import com.hjo2oa.org.org.sync.audit.application.TargetMasterDataResult;
import com.hjo2oa.org.org.sync.audit.domain.DiffRecord;
import org.springframework.stereotype.Component;

@Component
public class NoopTargetMasterDataGateway implements TargetMasterDataGateway {

    @Override
    public TargetMasterDataResult applyResolution(DiffRecord diffRecord, String actionType, String requestPayload) {
        return new TargetMasterDataResult(
                false,
                "No target master-data adapter is configured; diff is confirmed for manual governance"
        );
    }
}
