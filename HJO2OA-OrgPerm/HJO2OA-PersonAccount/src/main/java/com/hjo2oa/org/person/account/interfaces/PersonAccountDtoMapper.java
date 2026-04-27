package com.hjo2oa.org.person.account.interfaces;

import com.hjo2oa.org.person.account.domain.AccountView;
import com.hjo2oa.org.person.account.domain.PersonAccountView;
import com.hjo2oa.org.person.account.domain.PersonView;
import org.springframework.stereotype.Component;

@Component
public class PersonAccountDtoMapper {

    public PersonAccountDtos.PersonAccountResponse toPersonAccountResponse(PersonAccountView view) {
        return new PersonAccountDtos.PersonAccountResponse(
                toPersonResponse(view.person()),
                view.accounts().stream().map(this::toAccountResponse).toList()
        );
    }

    public PersonAccountDtos.PersonResponse toPersonResponse(PersonView view) {
        return new PersonAccountDtos.PersonResponse(
                view.id(),
                view.employeeNo(),
                view.name(),
                view.pinyin(),
                view.gender(),
                view.mobile(),
                view.email(),
                view.organizationId(),
                view.departmentId(),
                view.status(),
                view.tenantId(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    public PersonAccountDtos.AccountResponse toAccountResponse(AccountView view) {
        return new PersonAccountDtos.AccountResponse(
                view.id(),
                view.personId(),
                view.username(),
                view.accountType(),
                view.primaryAccount(),
                view.locked(),
                view.lockedUntil(),
                view.lastLoginAt(),
                view.lastLoginIp(),
                view.passwordChangedAt(),
                view.mustChangePassword(),
                view.status(),
                view.tenantId(),
                view.createdAt(),
                view.updatedAt()
        );
    }
}
