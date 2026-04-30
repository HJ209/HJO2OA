package com.hjo2oa.todo.center.infrastructure.persistence;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TodoProcessViewMapper {

    @Select("""
            SELECT TOP (100)
                pi.id AS instanceId,
                pi.definition_id AS definitionId,
                pi.definition_code AS definitionCode,
                pi.title AS title,
                pi.category AS category,
                pi.status AS status,
                pi.start_time AS startTime,
                pi.end_time AS endTime,
                pi.updated_at AS updatedAt
            FROM proc_instance pi
            WHERE pi.tenant_id = TRY_CONVERT(uniqueidentifier, #{tenantId})
              AND pi.initiator_id = TRY_CONVERT(uniqueidentifier, #{personId})
            ORDER BY pi.start_time DESC
            """)
    List<ProcessSummaryRow> findInitiated(@Param("tenantId") String tenantId, @Param("personId") String personId);

    @Select("""
            SELECT TOP (100)
                fs.submission_id AS submissionId,
                fs.metadata_id AS metadataId,
                fs.metadata_code AS metadataCode,
                fs.metadata_version AS metadataVersion,
                fs.process_instance_id AS processInstanceId,
                fs.node_id AS nodeId,
                fs.created_at AS createdAt,
                fs.updated_at AS updatedAt
            FROM wf_form_submission fs
            WHERE fs.tenant_id = TRY_CONVERT(uniqueidentifier, #{tenantId})
              AND fs.submitted_by = TRY_CONVERT(uniqueidentifier, #{personId})
              AND fs.status = 'DRAFT'
            ORDER BY fs.updated_at DESC
            """)
    List<DraftProcessSummaryRow> findDrafts(@Param("tenantId") String tenantId, @Param("personId") String personId);

    @Select("""
            SELECT TOP (100)
                pi.id AS instanceId,
                pi.definition_id AS definitionId,
                pi.definition_code AS definitionCode,
                pi.title AS title,
                pi.category AS category,
                pi.status AS status,
                pi.start_time AS startTime,
                pi.end_time AS endTime,
                pi.updated_at AS updatedAt
            FROM proc_instance pi
            WHERE pi.tenant_id = TRY_CONVERT(uniqueidentifier, #{tenantId})
              AND pi.initiator_id = TRY_CONVERT(uniqueidentifier, #{personId})
              AND pi.status IN ('COMPLETED', 'TERMINATED')
            ORDER BY COALESCE(pi.end_time, pi.updated_at) DESC
            """)
    List<ProcessSummaryRow> findArchives(@Param("tenantId") String tenantId, @Param("personId") String personId);
}
