package com.hjo2oa.org.person.account.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonRepository {

    Optional<Person> findById(UUID personId);

    Optional<Person> findByEmployeeNo(UUID tenantId, String employeeNo);

    List<Person> findByTenant(UUID tenantId);

    Person save(Person person);

    void deleteById(UUID personId);
}
