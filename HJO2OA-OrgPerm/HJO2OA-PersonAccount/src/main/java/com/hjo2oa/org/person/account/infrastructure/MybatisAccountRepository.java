package com.hjo2oa.org.person.account.infrastructure;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hjo2oa.org.person.account.domain.Account;
import com.hjo2oa.org.person.account.domain.AccountRepository;
import com.hjo2oa.org.person.account.domain.AccountStatus;
import com.hjo2oa.org.person.account.domain.AccountType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnBean(DataSource.class)
public class MybatisAccountRepository implements AccountRepository {

    private final AccountMapper accountMapper;

    public MybatisAccountRepository(AccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }

    @Override
    public Optional<Account> findById(UUID accountId) {
        return Optional.ofNullable(accountMapper.selectById(accountId)).map(this::toDomain);
    }

    @Override
    public Optional<Account> findByUsername(String username) {
        return Optional.ofNullable(accountMapper.selectOne(
                Wrappers.<AccountEntity>lambdaQuery().eq(AccountEntity::getUsername, username)
        )).map(this::toDomain);
    }

    @Override
    public List<Account> findByPersonId(UUID personId) {
        return accountMapper.selectList(
                Wrappers.<AccountEntity>lambdaQuery()
                        .eq(AccountEntity::getPersonId, personId)
                        .orderByDesc(AccountEntity::getPrimaryAccount)
                        .orderByAsc(AccountEntity::getAccountType)
        ).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Account> findByPersonIdAndType(UUID personId, AccountType accountType) {
        return Optional.ofNullable(accountMapper.selectOne(
                Wrappers.<AccountEntity>lambdaQuery()
                        .eq(AccountEntity::getPersonId, personId)
                        .eq(AccountEntity::getAccountType, accountType.name())
        )).map(this::toDomain);
    }

    @Override
    public Account save(Account account) {
        AccountEntity existing = accountMapper.selectById(account.id());
        AccountEntity entity = toEntity(account, existing);
        if (existing == null) {
            accountMapper.insert(entity);
        } else {
            accountMapper.updateById(entity);
        }
        return toDomain(accountMapper.selectById(account.id()));
    }

    @Override
    public void deleteById(UUID accountId) {
        accountMapper.deleteById(accountId);
    }

    private Account toDomain(AccountEntity entity) {
        return new Account(
                entity.getId(),
                entity.getPersonId(),
                entity.getUsername(),
                entity.getCredential(),
                AccountType.valueOf(entity.getAccountType()),
                Boolean.TRUE.equals(entity.getPrimaryAccount()),
                Boolean.TRUE.equals(entity.getLocked()),
                entity.getLockedUntil(),
                entity.getLastLoginAt(),
                entity.getLastLoginIp(),
                entity.getPasswordChangedAt(),
                Boolean.TRUE.equals(entity.getMustChangePassword()),
                AccountStatus.valueOf(entity.getStatus()),
                entity.getTenantId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private AccountEntity toEntity(Account account, AccountEntity existing) {
        AccountEntity entity = existing == null ? new AccountEntity() : existing;
        entity.setId(account.id());
        entity.setPersonId(account.personId());
        entity.setUsername(account.username());
        entity.setCredential(account.credential());
        entity.setAccountType(account.accountType().name());
        entity.setPrimaryAccount(account.primaryAccount());
        entity.setLocked(account.locked());
        entity.setLockedUntil(account.lockedUntil());
        entity.setLastLoginAt(account.lastLoginAt());
        entity.setLastLoginIp(account.lastLoginIp());
        entity.setPasswordChangedAt(account.passwordChangedAt());
        entity.setMustChangePassword(account.mustChangePassword());
        entity.setStatus(account.status().name());
        entity.setTenantId(account.tenantId());
        entity.setCreatedAt(account.createdAt());
        entity.setUpdatedAt(account.updatedAt());
        return entity;
    }
}
