package com.hjo2oa.org.identity.context.domain;

public interface IdentityContextSessionRepository {

    IdentityContextSession currentSession();

    IdentityContextSession save(IdentityContextSession session);
}
