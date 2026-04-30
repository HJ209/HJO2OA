package com.hjo2oa.org.person.account.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hjo2oa.org.person.account.domain.Account;
import com.hjo2oa.org.person.account.domain.AccountRepository;
import com.hjo2oa.org.person.account.domain.AccountStatus;
import com.hjo2oa.org.person.account.domain.AccountType;
import com.hjo2oa.org.person.account.domain.Person;
import com.hjo2oa.org.person.account.domain.PersonAccountView;
import com.hjo2oa.org.person.account.domain.PersonRepository;
import com.hjo2oa.org.person.account.domain.PersonStatus;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PersonAccountApplicationServiceTest {

    private static final UUID TENANT_A = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID TENANT_B = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID ORG_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    private InMemoryPersonRepository persons;
    private InMemoryAccountRepository accounts;
    private RecordingAssignmentLink assignmentLink;
    private PersonAccountApplicationService service;

    @BeforeEach
    void setUp() {
        persons = new InMemoryPersonRepository();
        accounts = new InMemoryAccountRepository();
        assignmentLink = new RecordingAssignmentLink();
        service = new PersonAccountApplicationService(
                persons,
                accounts,
                CLOCK,
                new BCryptPasswordEncoder(),
                new RecordingPublisher(),
                PersonAccountReferenceValidator.NOOP,
                assignmentLink
        );
    }

    @Test
    void passwordAccountIsHashedAndCanAuthenticate() {
        PersonAccountView person = createPerson("E001");

        service.createAccount(new PersonAccountCommands.CreateAccountCommand(
                person.person().id(),
                "e001",
                "secret",
                AccountType.PASSWORD,
                true,
                false,
                TENANT_A
        ));

        Account stored = accounts.findByUsername("e001").orElseThrow();
        assertThat(stored.credential()).isNotEqualTo("secret");
        assertThat(service.authenticate("e001", "secret", "127.0.0.1").tenantId()).isEqualTo(TENANT_A);
    }

    @Test
    void resignDisablesAccountsAndClosesActiveAssignments() {
        PersonAccountView person = createPerson("E001");
        service.createAccount(new PersonAccountCommands.CreateAccountCommand(
                person.person().id(),
                "e001",
                "secret",
                AccountType.PASSWORD,
                true,
                false,
                TENANT_A
        ));

        PersonAccountView resigned = service.resignPerson(TENANT_A, person.person().id());

        assertThat(resigned.person().status()).isEqualTo(PersonStatus.RESIGNED);
        assertThat(resigned.accounts()).allSatisfy(account -> assertThat(account.status()).isEqualTo(AccountStatus.DISABLED));
        assertThat(assignmentLink.closedTenantId).isEqualTo(TENANT_A);
        assertThat(assignmentLink.closedPersonId).isEqualTo(person.person().id());
        assertThat(assignmentLink.closedEndDate).isEqualTo(LocalDate.of(2026, 1, 1));
    }

    @Test
    void personUpdateIsScopedByTenant() {
        PersonAccountView person = createPerson("E001");

        assertThatThrownBy(() -> service.updatePerson(new PersonAccountCommands.UpdatePersonCommand(
                person.person().id(),
                "Other",
                null,
                null,
                null,
                null,
                ORG_ID,
                null,
                TENANT_B
        ))).isInstanceOf(BizException.class)
                .hasMessageContaining("not found");
    }

    private PersonAccountView createPerson(String employeeNo) {
        return service.createPerson(new PersonAccountCommands.CreatePersonCommand(
                employeeNo,
                "User " + employeeNo,
                null,
                null,
                null,
                null,
                ORG_ID,
                null,
                TENANT_A
        ));
    }

    private static final class InMemoryPersonRepository implements PersonRepository {

        private final Map<UUID, Person> rows = new HashMap<>();

        @Override
        public Optional<Person> findById(UUID personId) {
            return Optional.ofNullable(rows.get(personId));
        }

        @Override
        public Optional<Person> findByEmployeeNo(UUID tenantId, String employeeNo) {
            return rows.values().stream()
                    .filter(row -> row.tenantId().equals(tenantId))
                    .filter(row -> row.employeeNo().equals(employeeNo))
                    .findFirst();
        }

        @Override
        public List<Person> findByTenant(UUID tenantId) {
            return rows.values().stream()
                    .filter(row -> row.tenantId().equals(tenantId))
                    .toList();
        }

        @Override
        public Person save(Person person) {
            rows.put(person.id(), person);
            return person;
        }

        @Override
        public void deleteById(UUID personId) {
            rows.remove(personId);
        }
    }

    private static final class InMemoryAccountRepository implements AccountRepository {

        private final Map<UUID, Account> rows = new HashMap<>();

        @Override
        public Optional<Account> findById(UUID accountId) {
            return Optional.ofNullable(rows.get(accountId));
        }

        @Override
        public Optional<Account> findByUsername(String username) {
            return rows.values().stream()
                    .filter(row -> row.username().equals(username))
                    .findFirst();
        }

        @Override
        public List<Account> findByPersonId(UUID personId) {
            return rows.values().stream()
                    .filter(row -> row.personId().equals(personId))
                    .toList();
        }

        @Override
        public Optional<Account> findByPersonIdAndType(UUID personId, AccountType accountType) {
            return rows.values().stream()
                    .filter(row -> row.personId().equals(personId))
                    .filter(row -> row.accountType() == accountType)
                    .findFirst();
        }

        @Override
        public Account save(Account account) {
            rows.put(account.id(), account);
            return account;
        }

        @Override
        public void deleteById(UUID accountId) {
            rows.remove(accountId);
        }
    }

    private static final class RecordingAssignmentLink implements PersonAssignmentLink {

        private UUID closedTenantId;
        private UUID closedPersonId;
        private LocalDate closedEndDate;

        @Override
        public int closeActiveAssignments(UUID tenantId, UUID personId, LocalDate endDate, Instant changedAt) {
            closedTenantId = tenantId;
            closedPersonId = personId;
            closedEndDate = endDate;
            return 1;
        }
    }

    private static final class RecordingPublisher implements com.hjo2oa.shared.messaging.DomainEventPublisher {

        @Override
        public void publish(DomainEvent event) {
        }
    }
}
