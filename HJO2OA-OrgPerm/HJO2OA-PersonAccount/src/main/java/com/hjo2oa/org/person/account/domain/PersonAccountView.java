package com.hjo2oa.org.person.account.domain;

import java.util.List;

public record PersonAccountView(
        PersonView person,
        List<AccountView> accounts
) {
}
