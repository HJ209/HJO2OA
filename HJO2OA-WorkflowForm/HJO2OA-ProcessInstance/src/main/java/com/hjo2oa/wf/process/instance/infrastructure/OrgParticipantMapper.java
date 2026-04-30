package com.hjo2oa.wf.process.instance.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrgParticipantMapper {

    @Select("""
            <script>
            SELECT p.id AS personId, p.organization_id AS orgId, p.department_id AS deptId, a.position_id AS positionId
            FROM dbo.org_person p
            LEFT JOIN dbo.org_assignment a
              ON a.person_id = p.id
             AND a.tenant_id = p.tenant_id
             AND a.status = 'ACTIVE'
             AND a.type = 'PRIMARY'
            WHERE p.tenant_id = #{tenantId}
              AND p.status = 'ACTIVE'
              AND p.id IN
              <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>
            </script>
            """)
    List<OrgParticipantRow> findPeople(@Param("tenantId") UUID tenantId, @Param("ids") Collection<UUID> ids);

    @Select("""
            <script>
            SELECT p.id AS personId, p.organization_id AS orgId, p.department_id AS deptId, a.position_id AS positionId
            FROM dbo.org_person p
            LEFT JOIN dbo.org_assignment a
              ON a.person_id = p.id
             AND a.tenant_id = p.tenant_id
             AND a.status = 'ACTIVE'
             AND a.type = 'PRIMARY'
            WHERE p.tenant_id = #{tenantId}
              AND p.status = 'ACTIVE'
              AND p.organization_id IN
              <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>
            </script>
            """)
    List<OrgParticipantRow> findByOrganizations(@Param("tenantId") UUID tenantId, @Param("ids") Collection<UUID> ids);

    @Select("""
            <script>
            SELECT p.id AS personId, p.organization_id AS orgId, p.department_id AS deptId, a.position_id AS positionId
            FROM dbo.org_person p
            LEFT JOIN dbo.org_assignment a
              ON a.person_id = p.id
             AND a.tenant_id = p.tenant_id
             AND a.status = 'ACTIVE'
             AND a.type = 'PRIMARY'
            WHERE p.tenant_id = #{tenantId}
              AND p.status = 'ACTIVE'
              AND p.department_id IN
              <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>
            </script>
            """)
    List<OrgParticipantRow> findByDepartments(@Param("tenantId") UUID tenantId, @Param("ids") Collection<UUID> ids);

    @Select("""
            <script>
            SELECT p.id AS personId, p.organization_id AS orgId, p.department_id AS deptId, a.position_id AS positionId
            FROM dbo.org_assignment a
            JOIN dbo.org_person p ON p.id = a.person_id
            WHERE a.tenant_id = #{tenantId}
              AND p.tenant_id = #{tenantId}
              AND a.status = 'ACTIVE'
              AND p.status = 'ACTIVE'
              AND a.position_id IN
              <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>
            </script>
            """)
    List<OrgParticipantRow> findByPositions(@Param("tenantId") UUID tenantId, @Param("ids") Collection<UUID> ids);

    @Select("""
            <script>
            SELECT DISTINCT p.id AS personId, p.organization_id AS orgId, p.department_id AS deptId, a.position_id AS positionId
            FROM dbo.org_person_role pr
            JOIN dbo.org_person p ON p.id = pr.person_id
            LEFT JOIN dbo.org_assignment a
              ON a.person_id = p.id
             AND a.tenant_id = p.tenant_id
             AND a.status = 'ACTIVE'
             AND a.type = 'PRIMARY'
            WHERE pr.tenant_id = #{tenantId}
              AND p.tenant_id = #{tenantId}
              AND p.status = 'ACTIVE'
              AND pr.role_id IN
              <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>
            UNION
            SELECT DISTINCT p.id AS personId, p.organization_id AS orgId, p.department_id AS deptId, a.position_id AS positionId
            FROM dbo.org_position_role por
            JOIN dbo.org_assignment a ON a.position_id = por.position_id
            JOIN dbo.org_person p ON p.id = a.person_id
            WHERE por.tenant_id = #{tenantId}
              AND a.tenant_id = #{tenantId}
              AND p.tenant_id = #{tenantId}
              AND a.status = 'ACTIVE'
              AND p.status = 'ACTIVE'
              AND por.role_id IN
              <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>
            </script>
            """)
    List<OrgParticipantRow> findByRoles(@Param("tenantId") UUID tenantId, @Param("ids") Collection<UUID> ids);

    @Select("""
            <script>
            SELECT p.id AS personId, p.organization_id AS orgId, p.department_id AS deptId, a.position_id AS positionId
            FROM dbo.org_department d
            JOIN dbo.org_person p ON p.id = d.manager_id
            LEFT JOIN dbo.org_assignment a
              ON a.person_id = p.id
             AND a.tenant_id = p.tenant_id
             AND a.status = 'ACTIVE'
             AND a.type = 'PRIMARY'
            WHERE d.tenant_id = #{tenantId}
              AND p.tenant_id = #{tenantId}
              AND p.status = 'ACTIVE'
              AND d.id IN
              <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>
            </script>
            """)
    List<OrgParticipantRow> findDepartmentManagers(@Param("tenantId") UUID tenantId, @Param("ids") Collection<UUID> ids);
}
