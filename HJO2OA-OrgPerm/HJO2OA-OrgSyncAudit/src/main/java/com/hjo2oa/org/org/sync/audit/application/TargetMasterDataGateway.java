package com.hjo2oa.org.org.sync.audit.application;

import com.hjo2oa.org.org.sync.audit.domain.DiffRecord;

public interface TargetMasterDataGateway {

    TargetMasterDataResult applyResolution(DiffRecord diffRecord, String actionType, String requestPayload);
}
