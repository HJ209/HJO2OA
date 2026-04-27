package com.hjo2oa.org.person.account.infrastructure;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hjo2oa.org.person.account.domain.Person;
import com.hjo2oa.org.person.account.domain.PersonRepository;
import com.hjo2oa.org.person.account.domain.PersonStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnBean(DataSource.class)
public class MybatisPersonRepository implements PersonRepository {

    private final PersonMapper personMapper;

    public MybatisPersonRepository(PersonMapper personMapper) {
        this.personMapper = personMapper;
    }

    @Override
    public Optional<Person> findById(UUID personId) {
        return Optional.ofNullable(personMapper.selectById(personId)).map(this::toDomain);
    }

    @Override
    public Optional<Person> findByEmployeeNo(UUID tenantId, String employeeNo) {
        return Optional.ofNullable(personMapper.selectOne(
                Wrappers.<PersonEntity>lambdaQuery()
                        .eq(PersonEntity::getTenantId, tenantId)
                        .eq(PersonEntity::getEmployeeNo, employeeNo)
        )).map(this::toDomain);
    }

    @Override
    public List<Person> findByTenant(UUID tenantId) {
        return personMapper.selectList(
                Wrappers.<PersonEntity>lambdaQuery()
                        .eq(PersonEntity::getTenantId, tenantId)
                        .orderByAsc(PersonEntity::getEmployeeNo)
        ).stream().map(this::toDomain).toList();
    }

    @Override
    public Person save(Person person) {
        PersonEntity existing = personMapper.selectById(person.id());
        PersonEntity entity = toEntity(person, existing);
        if (existing == null) {
            personMapper.insert(entity);
        } else {
            personMapper.updateById(entity);
        }
        return toDomain(personMapper.selectById(person.id()));
    }

    @Override
    public void deleteById(UUID personId) {
        personMapper.deleteById(personId);
    }

    private Person toDomain(PersonEntity entity) {
        return new Person(
                entity.getId(),
                entity.getEmployeeNo(),
                entity.getName(),
                entity.getPinyin(),
                entity.getGender(),
                entity.getMobile(),
                entity.getEmail(),
                entity.getOrganizationId(),
                entity.getDepartmentId(),
                PersonStatus.valueOf(entity.getStatus()),
                entity.getTenantId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private PersonEntity toEntity(Person person, PersonEntity existing) {
        PersonEntity entity = existing == null ? new PersonEntity() : existing;
        entity.setId(person.id());
        entity.setEmployeeNo(person.employeeNo());
        entity.setName(person.name());
        entity.setPinyin(person.pinyin());
        entity.setGender(person.gender());
        entity.setMobile(person.mobile());
        entity.setEmail(person.email());
        entity.setOrganizationId(person.organizationId());
        entity.setDepartmentId(person.departmentId());
        entity.setStatus(person.status().name());
        entity.setTenantId(person.tenantId());
        entity.setCreatedAt(person.createdAt());
        entity.setUpdatedAt(person.updatedAt());
        return entity;
    }
}
