package com.hjo2oa.org.person.account.application;

import com.hjo2oa.org.person.account.domain.Account;
import com.hjo2oa.org.person.account.domain.AccountRepository;
import com.hjo2oa.org.person.account.domain.AccountStatus;
import com.hjo2oa.org.person.account.domain.AccountType;
import com.hjo2oa.org.person.account.domain.AccountView;
import com.hjo2oa.org.person.account.domain.Person;
import com.hjo2oa.org.person.account.domain.PersonAccountView;
import com.hjo2oa.org.person.account.domain.PersonAccountChangedEvent;
import com.hjo2oa.org.person.account.domain.PersonRepository;
import com.hjo2oa.org.person.account.domain.PersonStatus;
import com.hjo2oa.org.person.account.domain.PersonView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import com.hjo2oa.shared.tenant.TenantContextHolder;
import com.hjo2oa.shared.tenant.TenantRequestContext;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

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
    private final DomainEventPublisher domainEventPublisher;
    private final PersonAccountReferenceValidator referenceValidator;
    private final PersonAssignmentLink assignmentLink;

    @Autowired
    public PersonAccountApplicationService(
            PersonRepository personRepository,
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            ObjectProvider<DomainEventPublisher> domainEventPublisherProvider,
            ObjectProvider<PersonAccountReferenceValidator> referenceValidatorProvider,
            ObjectProvider<PersonAssignmentLink> assignmentLinkProvider
    ) {
        this(
                personRepository,
                accountRepository,
                Clock.systemUTC(),
                passwordEncoder,
                domainEventPublisherProvider.getIfAvailable(() -> event -> { }),
                referenceValidatorProvider.getIfAvailable(() -> PersonAccountReferenceValidator.NOOP),
                assignmentLinkProvider.getIfAvailable(() -> PersonAssignmentLink.NOOP)
        );
    }

    public PersonAccountApplicationService(
            PersonRepository personRepository,
            AccountRepository accountRepository,
            Clock clock
    ) {
        this(
                personRepository,
                accountRepository,
                clock,
                new BCryptPasswordEncoder(),
                event -> { },
                PersonAccountReferenceValidator.NOOP,
                PersonAssignmentLink.NOOP
        );
    }

    public PersonAccountApplicationService(
            PersonRepository personRepository,
            AccountRepository accountRepository,
            Clock clock,
            PasswordEncoder passwordEncoder
    ) {
        this(
                personRepository,
                accountRepository,
                clock,
                passwordEncoder,
                event -> { },
                PersonAccountReferenceValidator.NOOP,
                PersonAssignmentLink.NOOP
        );
    }

    public PersonAccountApplicationService(
            PersonRepository personRepository,
            AccountRepository accountRepository,
            Clock clock,
            PasswordEncoder passwordEncoder,
            DomainEventPublisher domainEventPublisher,
            PersonAccountReferenceValidator referenceValidator,
            PersonAssignmentLink assignmentLink
    ) {
        this.personRepository = Objects.requireNonNull(personRepository, "personRepository must not be null");
        this.accountRepository = Objects.requireNonNull(accountRepository, "accountRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder must not be null");
        this.domainEventPublisher = Objects.requireNonNull(domainEventPublisher, "domainEventPublisher must not be null");
        this.referenceValidator = Objects.requireNonNull(referenceValidator, "referenceValidator must not be null");
        this.assignmentLink = Objects.requireNonNull(assignmentLink, "assignmentLink must not be null");
    }

    @Transactional
    public PersonAccountView createPerson(PersonAccountCommands.CreatePersonCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        referenceValidator.ensureOrgScopeActive(command.tenantId(), command.organizationId(), command.departmentId());
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
        Person saved = personRepository.save(person);
        publishPersonEvent("org.person.created", saved, payloadOf("organizationId", saved.organizationId()));
        return toPersonAccountView(saved);
    }

    @Transactional
    public PersonAccountView updatePerson(PersonAccountCommands.UpdatePersonCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        referenceValidator.ensureOrgScopeActive(command.tenantId(), command.organizationId(), command.departmentId());
        Person person = loadRequiredPerson(command.tenantId(), command.personId()).updateProfile(
                command.name(),
                command.pinyin(),
                command.gender(),
                command.mobile(),
                command.email(),
                command.organizationId(),
                command.departmentId(),
                now()
        );
        Person saved = personRepository.save(person);
        publishPersonEvent("org.person.updated", saved, payloadOf("organizationId", saved.organizationId()));
        return toPersonAccountView(saved);
    }

    public PersonAccountView getPerson(UUID personId) {
        return toPersonAccountView(loadRequiredPerson(personId));
    }

    public PersonAccountView getPerson(UUID tenantId, UUID personId) {
        return toPersonAccountView(loadRequiredPerson(tenantId, personId));
    }

    public List<PersonView> listPersons(UUID tenantId) {
        return personRepository.findByTenant(tenantId).stream()
                .sorted(PERSON_ORDER)
                .map(Person::toView)
                .toList();
    }

    @Transactional
    public PersonAccountView disablePerson(UUID tenantId, UUID personId) {
        Instant now = now();
        Person person = personRepository.save(loadRequiredPerson(tenantId, personId).disable(now));
        int closedAssignments = assignmentLink.closeActiveAssignments(tenantId, personId, LocalDate.now(clock), now);
        for (Account account : accountRepository.findByPersonId(personId)) {
            if (account.tenantId().equals(tenantId)) {
                Account saved = accountRepository.save(account.disable(now));
                publishAccountEvent("org.account.disabled", saved, payloadOf("personId", personId));
            }
        }
        publishPersonEvent("org.person.disabled", person, payloadOf("closedAssignments", closedAssignments));
        return toPersonAccountView(person);
    }

    @Transactional
    public PersonAccountView activatePerson(UUID tenantId, UUID personId) {
        Person current = loadRequiredPerson(tenantId, personId);
        if (current.status() == PersonStatus.RESIGNED) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Resigned person cannot be reactivated");
        }
        referenceValidator.ensureOrgScopeActive(current.tenantId(), current.organizationId(), current.departmentId());
        Person person = personRepository.save(current.activate(now()));
        publishPersonEvent("org.person.enabled", person, Map.of());
        return toPersonAccountView(person);
    }

    @Transactional
    public PersonAccountView resignPerson(UUID tenantId, UUID personId) {
        Instant now = now();
        Person person = personRepository.save(loadRequiredPerson(tenantId, personId).resign(now));
        int closedAssignments = assignmentLink.closeActiveAssignments(tenantId, personId, LocalDate.now(clock), now);
        for (Account account : accountRepository.findByPersonId(personId)) {
            if (account.tenantId().equals(tenantId)) {
                Account saved = accountRepository.save(account.disable(now));
                publishAccountEvent("org.account.disabled", saved, payloadOf("personId", personId));
            }
        }
        publishPersonEvent("org.person.resigned", person, payloadOf("closedAssignments", closedAssignments));
        if (closedAssignments > 0) {
            publishPersonEvent("org.assignment.ended", person, payloadOf("closedAssignments", closedAssignments));
        }
        return toPersonAccountView(person);
    }

    @Transactional
    public void deletePerson(UUID tenantId, UUID personId) {
        Person person = loadRequiredPerson(tenantId, personId);
        referenceValidator.ensurePersonCanBeDeleted(tenantId, personId);
        for (Account account : accountRepository.findByPersonId(personId)) {
            if (account.tenantId().equals(tenantId)) {
                accountRepository.deleteById(account.id());
                publishAccountEvent("org.account.deleted", account, payloadOf("personId", personId));
            }
        }
        personRepository.deleteById(personId);
        publishPersonEvent("org.person.deleted", person, Map.of());
    }

    @Transactional
    public PersonAccountView createAccount(PersonAccountCommands.CreateAccountCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Person person = loadRequiredPerson(command.tenantId(), command.personId());
        if (person.status() != PersonStatus.ACTIVE) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Person is not active");
        }
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
                encodedCredential(command.accountType(), command.credential()),
                command.accountType(),
                makePrimary,
                command.mustChangePassword(),
                person.tenantId(),
                now()
        );
        Account saved = accountRepository.save(account);
        publishAccountEvent("org.account.bound", saved, payloadOf("personId", person.id()));
        return toPersonAccountView(person);
    }

    @Transactional
    public AccountView updateAccountCredential(
            PersonAccountCommands.UpdateAccountCredentialCommand command
    ) {
        Objects.requireNonNull(command, "command must not be null");
        Account current = loadRequiredAccount(command.tenantId(), command.accountId());
        Account account = current
                .updateCredential(
                        encodedCredential(current.accountType(), command.credential()),
                        command.mustChangePassword(),
                        now()
                );
        Account saved = accountRepository.save(account);
        publishAccountEvent("org.account.reset", saved, Map.of());
        return saved.toView();
    }

    @Transactional
    public AccountView lockAccount(PersonAccountCommands.LockAccountCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Account account = loadRequiredAccount(command.tenantId(), command.accountId()).lock(command.lockedUntil(), now());
        Account saved = accountRepository.save(account);
        publishAccountEvent("org.account.locked", saved, payloadOf("lockedUntil", saved.lockedUntil()));
        return saved.toView();
    }

    @Transactional
    public AccountView unlockAccount(UUID tenantId, UUID accountId) {
        Account saved = accountRepository.save(loadRequiredAccount(tenantId, accountId).unlock(now()));
        publishAccountEvent("org.account.unlocked", saved, Map.of());
        return saved.toView();
    }

    @Transactional
    public AccountView disableAccount(UUID tenantId, UUID accountId) {
        Account saved = accountRepository.save(loadRequiredAccount(tenantId, accountId).disable(now()));
        publishAccountEvent("org.account.disabled", saved, Map.of());
        return saved.toView();
    }

    @Transactional
    public AccountView activateAccount(UUID tenantId, UUID accountId) {
        Account account = loadRequiredAccount(tenantId, accountId);
        Person person = loadRequiredPerson(tenantId, account.personId());
        if (person.status() != PersonStatus.ACTIVE) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Person is not active");
        }
        Account saved = accountRepository.save(account.activate(now()));
        publishAccountEvent("org.account.enabled", saved, Map.of());
        return saved.toView();
    }

    @Transactional
    public PersonAccountView setPrimaryAccount(UUID tenantId, UUID personId, UUID accountId) {
        Person person = loadRequiredPerson(tenantId, personId);
        Account target = loadRequiredAccount(tenantId, accountId);
        if (!personId.equals(target.personId())) {
            throw new BizException(SharedErrorDescriptors.BUSINESS_RULE_VIOLATION, "Account does not belong to person");
        }
        clearPrimaryAccounts(personId);
        Account saved = accountRepository.save(target.markPrimary(true, now()));
        publishAccountEvent("org.account.primary_changed", saved, payloadOf("personId", personId));
        return toPersonAccountView(person);
    }

    @Transactional
    public AccountView recordLogin(PersonAccountCommands.RecordLoginCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Account account = loadRequiredAccount(command.tenantId(), command.accountId()).recordLogin(command.loginIp(), now());
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

    @Transactional
    public void deleteAccount(UUID tenantId, UUID accountId) {
        Account account = loadRequiredAccount(tenantId, accountId);
        if (account.primaryAccount() && accountRepository.findByPersonId(account.personId()).size() > 1) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    "Primary account must be reassigned before deletion"
            );
        }
        accountRepository.deleteById(accountId);
        publishAccountEvent("org.account.deleted", account, payloadOf("personId", account.personId()));
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
        accountRepository.findByUsername(requireText(username, "username"))
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

    private Person loadRequiredPerson(UUID tenantId, UUID personId) {
        Person person = loadRequiredPerson(personId);
        if (!person.tenantId().equals(tenantId)) {
            throw new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Person not found");
        }
        return person;
    }

    private Account loadRequiredAccount(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Account not found"));
    }

    private Account loadRequiredAccount(UUID tenantId, UUID accountId) {
        Account account = loadRequiredAccount(accountId);
        if (!account.tenantId().equals(tenantId)) {
            throw new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Account not found");
        }
        return account;
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

    private String encodedCredential(AccountType accountType, String credential) {
        String normalized = requireText(credential, "credential");
        if (accountType == AccountType.PASSWORD) {
            return passwordEncoder.encode(normalized);
        }
        return normalized;
    }

    private void publishPersonEvent(String eventType, Person person, Map<String, Object> details) {
        domainEventPublisher.publish(PersonAccountChangedEvent.of(
                eventType,
                person.tenantId(),
                person.id(),
                now(),
                eventPayload(details)
        ));
    }

    private void publishAccountEvent(String eventType, Account account, Map<String, Object> details) {
        domainEventPublisher.publish(PersonAccountChangedEvent.of(
                eventType,
                account.tenantId(),
                account.id(),
                now(),
                eventPayload(details)
        ));
    }

    private Map<String, Object> eventPayload(Map<String, Object> details) {
        TenantRequestContext context = TenantContextHolder.current().orElse(null);
        Map<String, Object> payload = new LinkedHashMap<>();
        if (context != null) {
            putIfNotNull(payload, "requestId", context.requestId());
            putIfNotNull(payload, "idempotencyKey", context.idempotencyKey());
            putIfNotNull(payload, "language", context.language().toLanguageTag());
            putIfNotNull(payload, "timezone", context.timezone().getId());
            putIfNotNull(payload, "identityAssignmentId", context.identityAssignmentId());
            putIfNotNull(payload, "identityPositionId", context.identityPositionId());
        }
        if (details != null) {
            details.forEach((key, value) -> putIfNotNull(payload, key, value));
        }
        return payload;
    }

    private Map<String, Object> payloadOf(Object... keysAndValues) {
        Map<String, Object> payload = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keysAndValues.length; i += 2) {
            Object key = keysAndValues[i];
            if (key instanceof String name) {
                putIfNotNull(payload, name, keysAndValues[i + 1]);
            }
        }
        return payload;
    }

    private void putIfNotNull(Map<String, Object> payload, String key, Object value) {
        if (value != null) {
            payload.put(key, value);
        }
    }

    public record AuthenticatedAccount(
            UUID accountId,
            UUID personId,
            String username,
            UUID tenantId
    ) {
    }
}
