package com.hjo2oa.portal.personalization.infrastructure;

import com.hjo2oa.portal.personalization.domain.PersonalizationProfile;
import com.hjo2oa.portal.personalization.domain.PersonalizationProfileKey;
import com.hjo2oa.portal.personalization.domain.PersonalizationProfileRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryPersonalizationProfileRepository implements PersonalizationProfileRepository {

    private final Map<PersonalizationProfileKey, PersonalizationProfile> profiles = new ConcurrentHashMap<>();

    @Override
    public Optional<PersonalizationProfile> findByKey(PersonalizationProfileKey key) {
        return Optional.ofNullable(profiles.get(key));
    }

    @Override
    public PersonalizationProfile save(PersonalizationProfile profile) {
        profiles.put(profile.key(), profile);
        return profile;
    }
}
