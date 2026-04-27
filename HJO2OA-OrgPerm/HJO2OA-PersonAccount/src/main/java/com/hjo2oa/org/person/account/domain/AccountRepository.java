package com.hjo2oa.org.person.account.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {

    Optional<Account> findById(UUID accountId);

    Optional<Account> findByUsername(String username);

    List<Account> findByPersonId(UUID personId);

    Optional<Account> findByPersonIdAndType(UUID personId, AccountType accountType);

    Account save(Account account);

    void deleteById(UUID accountId);
}
