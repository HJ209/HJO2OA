package com.hjo2oa.portal.personalization.domain;

import java.util.Optional;

public interface PersonalizationProfileRepository {

    Optional<PersonalizationProfile> findByKey(PersonalizationProfileKey key);

    PersonalizationProfile save(PersonalizationProfile profile);
}
