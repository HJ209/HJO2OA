package com.hjo2oa.org.person.account.application;

import com.hjo2oa.org.person.account.domain.Account;
import com.hjo2oa.org.person.account.domain.AccountRepository;
import com.hjo2oa.org.person.account.domain.AccountStatus;
import com.hjo2oa.org.person.account.domain.AccountType;
import com.hjo2oa.org.person.account.domain.AccountView;
import com.hjo2oa.org.person.account.domain.Person;
import com.hjo2oa.org.person.account.domain.PersonAccountView;
import com.hjo2oa.org.person.account.domain.PersonRepository;
import com.hjo2oa.org.person.account.domain.PersonView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class PersonAccountApplicationService {

    private static final Comparator<Person> PERSON_ORDER = Comparator
            .comparing(Person::employeeNo)
            .thenComparing(Person::name);
    private static final Comparator<Account> ACCOUNT_ORDER = Comparator
            .comparing(Account::primaryAccount)
            .reversed()
            .thenComparing(account -> account.accountType().name())
            .thenComparing(Account::username);

    private final PersonRepository personRepository;
    private final AccountRepository accountRepository;
    private final Clock clock;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public PersonAccountApplicationService(
            PersonRepository personRepository,
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder
    ) {
        this(personRepository, accountRepository, Clock.systemUTC(), passwordEncoder);
    }

    public PersonAccountApplicationService(
            PersonRepository personRepository,
            AccountRepository accountRepository,
            Clock clock
    ) {
        this(personRepository, accountRepository, clock, new BCryptPasswordEncoder());
    }

    public PersonAccountApplicationService(
            PersonRepository personRepository,
            AccountRepository accountRepository,
            Clock clock,
            PasswordEncoder passwordEncoder
    ) {
        this.personRepository = Objects.requireNonNull(personRepository, "personRepository must not be null");
        this.accountRepository = Objects.requireNonNull(accountRepository, "accountRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder must not be null");
    }

    public PersonAccountView createPerson(PersonAccountCommands.CreatePersonCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        personRepository.findByEmployeeNo(command.tenantId(), command.employeeNo())
                .ifPresent(existing -> {
                    throw new BizException(SharedErrorDescriptors.CONFLICT, "Person employeeNo already exists");
                });
        Person person = Person.create(
                UUID.randomUUID(),
                command.employeeNo(),
                command.name(),
                command.pinyin(),
                command.gender(),
                command.mobile(),
                command.email(),
                command.organizationId(),
                command.departmentId(),
                command.tenantId(),
                now()
        );
        return toPersonAccountView(personRepository.save(person));
    }

    public PersonAccountView updatePerson(PersonAccountCommands.UpdatePersonCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Person person = loadRequiredPerson(command.personId()).updateProfile(
                command.name(),
                command.pinyin(),
                command.gender(),
                command.mobile(),
                command.email(),
                command.organizationId(),
                command.departmentId(),
                now()
        );
        return toPersonAccountView(personRepository.save(person));
    }

    public PersonAccountView getPerson(UUID personId) {
        return toPersonAccountView(loadRequiredPerson(personId));
    }

    public List<PersonView> listPersons(UUID tenantId) {
        return personRepository.findByTenant(tenantId).stream()
                .sorted(PERSON_ORDER)
                .map(Person::toView)
                .toList();
    }

    public PersonAccountView disablePerson(UUID personId) {
        Person person = personRepository.save(loadRequiredPerson(personId).disable(now()));
        for (Account account : accountRepository.findByPersonId(personId)) {
            accountRepository.save(account.disable(now()));
        }
        return toPersonAccountView(person);
    }

    public PersonAccountView resignPerson(UUID personId) {
        Person person = personRepository.save(loadRequiredPerson(personId).resign(now()));
        for (Account account : accountRepository.findByPersonId(personId)) {
            accountRepository.save(account.disable(now()));
        }
        return toPersonAccountView(person);
    }

    public void deletePerson(UUID personId) {
        for (Account account : accountRepository.findByPersonId(personId)) {
            accountRepository.deleteById(account.id());
        }
        personRepository.deleteById(personId);
    }

    public PersonAccountView createAccount(PersonAccountCommands.CreateAccountCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Person person = loadRequiredPerson(command.personId());
        ensureUniqueUsername(command.username());
        accountRepository.findByPersonIdAndType(command.personId(), command.accountType())
                .ifPresent(existing -> {
                    throw new BizException(SharedErrorDescriptors.CONFLICT, "Person already has this account type");
                });
        boolean makePrimary = command.primaryAccount()
                || accountRepository.findByPersonId(command.personId()).isEmpty();
        if (makePrimary) {
            clearPrimaryAccounts(command.personId());
        }
        Account account = Account.create(
                UUID.randomUUID(),
                command.personId(),
                command.username(),
                command.credential(),
                command.accountType(),
                makePrimary,
                command.mustChangePassword(),
                person.tenantId(),
                now()
        );
        accountRepository.save(account);
        return toPersonAccountView(person);
    }

    public AccountView updateAccountCredential(
            PersonAccountCommands.UpdateAccountCredentialCommand command
    ) {
        Objects.requireNonNull(command, "command must not be null");
        Account account = loadRequiredAccount(command.accountId())
                .updateCredential(command.credential(), command.mustChangePassword(), now());
        return accountRepository.save(account).toView();
    }

    public AccountView lockAccount(PersonAccountCommands.LockAccountCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Account account = loadRequiredAccount(command.accountId()).lock(command.lockedUntil(), now());
        return accountRepository.save(account).toView();
    }

    public AccountView unlockAccount(UUID accountId) {
        return accountRepository.save(loadRequiredAccount(accountId).unlock(now())).toView();
    }

    public PersonAccountView setPrimaryAccount(UUID personId, UUID accountId) {
        Person person = loadRequiredPerson(personId);
        Account target = loadRequiredAccount(accountId);
        if (!personId.equals(target.personId())) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Account does not belong to person");
        }
        clearPrimaryAccounts(personId);
        accountRepository.save(target.markPrimary(true, now()));
        return toPersonAccountView(person);
    }

    public AccountView recordLogin(PersonAccountCommands.RecordLoginCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Account account = loadRequiredAccount(command.accountId()).recordLogin(command.loginIp(), now());
        return accountRepository.save(account).toView();
    }

    public AuthenticatedAccount authenticate(String username, String rawPassword, String loginIp) {
        String normalizedUsername = requireText(username, "username");
        String normalizedPassword = requireText(rawPassword, "password");
        Account account = accountRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.UNAUTHORIZED, "Invalid username or password"));
        if (account.accountType() != AccountType.PASSWORD
                || account.status() != AccountStatus.ACTIVE
                || account.locked()
                || !passwordEncoder.matches(normalizedPassword, account.credential())) {
            throw new BizException(SharedErrorDescriptors.UNAUTHORIZED, "Invalid username or password");
        }
        Account loggedIn = accountRepository.save(account.recordLogin(loginIp, now()));
        return new AuthenticatedAccount(
                loggedIn.id(),
                loggedIn.personId(),
                loggedIn.username(),
                loggedIn.tenantId()
        );
    }

    public void deleteAccount(UUID accountId) {
        Account account = loadRequiredAccount(accountId);
        if (account.primaryAccount() && accountRepository.findByPersonId(account.personId()).size() > 1) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    "Primary account must be reassigned before deletion"
            );
        }
        accountRepository.deleteById(accountId);
    }

    private PersonAccountView toPersonAccountView(Person person) {
        return new PersonAccountView(
                person.toView(),
                accountRepository.findByPersonId(person.id()).stream()
                        .sorted(ACCOUNT_ORDER)
                        .map(Account::toView)
                        .toList()
        );
    }

    private void ensureUniqueUsername(String username) {
        accountRepository.findByUsername(username)
                .ifPresent(existing -> {
                    throw new BizException(SharedErrorDescriptors.CONFLICT, "Account username already exists");
                });
    }

    private void clearPrimaryAccounts(UUID personId) {
        for (Account account : accountRepository.findByPersonId(personId)) {
            if (account.primaryAccount()) {
                accountRepository.save(account.markPrimary(false, now()));
            }
        }
    }

    private Person loadRequiredPerson(UUID personId) {
        return personRepository.findById(personId)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Person not found"));
    }

    private Account loadRequiredAccount(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Account not found"));
    }

    private Instant now() {
        return clock.instant();
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    public record AuthenticatedAccount(
            UUID accountId,
            UUID personId,
            String username,
            UUID tenantId
    ) {
    }
}
