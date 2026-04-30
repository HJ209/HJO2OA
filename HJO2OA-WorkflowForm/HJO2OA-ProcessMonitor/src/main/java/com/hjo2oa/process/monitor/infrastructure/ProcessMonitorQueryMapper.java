package com.hjo2oa.process.monitor.infrastructure;

import com.hjo2oa.process.monitor.domain.MonitorQueryFilter;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProcessMonitorQueryMapper {

    @Select("""
            <script>
            SELECT TOP (#{filter.limit})
                pi.definition_id AS definitionId,
                pi.definition_code AS definitionCode,
                pi.category AS category,
                COUNT(1) AS instanceCount,
                SUM(CASE WHEN pi.status = 'COMPLETED' THEN 1 ELSE 0 END) AS completedCount,
                SUM(CASE WHEN pi.status = 'RUNNING' THEN 1 ELSE 0 END) AS runningCount,
                COALESCE(AVG(CAST(DATEDIFF_BIG(MINUTE, pi.start_time,
                    COALESCE(pi.end_time, SYSUTCDATETIME())) AS BIGINT)), 0) AS averageDurationMinutes,
                COALESCE(MAX(DATEDIFF_BIG(MINUTE, pi.start_time,
                    COALESCE(pi.end_time, SYSUTCDATETIME()))), 0) AS maxDurationMinutes
            FROM proc_instance pi
            <where>
                <if test="filter.tenantId != null">
                    AND pi.tenant_id = #{filter.tenantId}
                </if>
                <if test="filter.definitionId != null">
                    AND pi.definition_id = #{filter.definitionId}
                </if>
                <if test="filter.definitionCode != null">
                    AND pi.definition_code = #{filter.definitionCode}
                </if>
                <if test="filter.category != null">
                    AND pi.category = #{filter.category}
                </if>
                <if test="filter.startedFrom != null">
                    AND pi.start_time &gt;= #{filter.startedFrom}
                </if>
                <if test="filter.startedTo != null">
                    AND pi.start_time &lt; #{filter.startedTo}
                </if>
            </where>
            GROUP BY pi.definition_id, pi.definition_code, pi.category
            ORDER BY averageDurationMinutes DESC, instanceCount DESC
            </script>
            """)
    List<ProcessDurationAnalysisRow> analyzeProcessDurations(@Param("filter") MonitorQueryFilter filter);

    @Select("""
            <script>
            SELECT TOP (#{filter.limit})
                pt.id AS taskId,
                pt.instance_id AS instanceId,
                pi.title AS instanceTitle,
                pi.definition_id AS definitionId,
                pi.definition_code AS definitionCode,
                pi.category AS category,
                pt.node_id AS nodeId,
                pt.node_name AS nodeName,
                pt.assignee_id AS assigneeId,
                pt.status AS taskStatus,
                pt.created_at AS taskCreatedAt,
                pt.due_time AS dueTime,
                DATEDIFF_BIG(MINUTE, pt.created_at, SYSUTCDATETIME()) AS stalledMinutes
            FROM proc_task pt
            INNER JOIN proc_instance pi ON pi.id = pt.instance_id
            <where>
                pt.status IN ('CREATED', 'CLAIMED')
                AND DATEDIFF_BIG(MINUTE, pt.created_at, SYSUTCDATETIME())
                    &gt;= #{filter.stalledThresholdMinutes}
                <if test="filter.tenantId != null">
                    AND pi.tenant_id = #{filter.tenantId}
                </if>
                <if test="filter.definitionId != null">
                    AND pi.definition_id = #{filter.definitionId}
                </if>
                <if test="filter.definitionCode != null">
                    AND pi.definition_code = #{filter.definitionCode}
                </if>
                <if test="filter.category != null">
                    AND pi.category = #{filter.category}
                </if>
                <if test="filter.startedFrom != null">
                    AND pi.start_time &gt;= #{filter.startedFrom}
                </if>
                <if test="filter.startedTo != null">
                    AND pi.start_time &lt; #{filter.startedTo}
                </if>
            </where>
            ORDER BY stalledMinutes DESC, pt.created_at ASC
            </script>
            """)
    List<NodeStagnationAnalysisRow> findStalledNodes(@Param("filter") MonitorQueryFilter filter);

    @Select("""
            <script>
            SELECT TOP (#{filter.limit})
                pt.assignee_id AS assigneeId,
                COUNT(1) AS pendingCount,
                SUM(CASE WHEN pt.due_time IS NOT NULL
                    AND pt.due_time &lt; SYSUTCDATETIME() THEN 1 ELSE 0 END) AS overdueCount,
                MIN(pt.created_at) AS oldestPendingAt,
                MIN(pt.due_time) AS nearestDueTime
            FROM proc_task pt
            INNER JOIN proc_instance pi ON pi.id = pt.instance_id
            <where>
                pt.status IN ('CREATED', 'CLAIMED')
                AND pt.assignee_id IS NOT NULL
                <if test="filter.tenantId != null">
                    AND pi.tenant_id = #{filter.tenantId}
                </if>
                <if test="filter.definitionId != null">
                    AND pi.definition_id = #{filter.definitionId}
                </if>
                <if test="filter.definitionCode != null">
                    AND pi.definition_code = #{filter.definitionCode}
                </if>
                <if test="filter.category != null">
                    AND pi.category = #{filter.category}
                </if>
                <if test="filter.startedFrom != null">
                    AND pi.start_time &gt;= #{filter.startedFrom}
                </if>
                <if test="filter.startedTo != null">
                    AND pi.start_time &lt; #{filter.startedTo}
                </if>
            </where>
            GROUP BY pt.assignee_id
            ORDER BY pendingCount DESC, overdueCount DESC, oldestPendingAt ASC
            </script>
            """)
    List<ApprovalCongestionAnalysisRow> rankApprovalCongestion(@Param("filter") MonitorQueryFilter filter);

    @Select("""
            <script>
            SELECT TOP (#{filter.limit})
                pt.id AS taskId,
                pt.instance_id AS instanceId,
                pi.title AS instanceTitle,
                pi.definition_id AS definitionId,
                pi.definition_code AS definitionCode,
                pi.category AS category,
                pt.node_id AS nodeId,
                pt.node_name AS nodeName,
                pt.assignee_id AS assigneeId,
                pt.due_time AS dueTime,
                DATEDIFF_BIG(MINUTE, pt.due_time, SYSUTCDATETIME()) AS overdueMinutes
            FROM proc_task pt
            INNER JOIN proc_instance pi ON pi.id = pt.instance_id
            <where>
                pt.status IN ('CREATED', 'CLAIMED')
                AND pt.due_time IS NOT NULL
                AND pt.due_time &lt; SYSUTCDATETIME()
                <if test="filter.tenantId != null">
                    AND pi.tenant_id = #{filter.tenantId}
                </if>
                <if test="filter.definitionId != null">
                    AND pi.definition_id = #{filter.definitionId}
                </if>
                <if test="filter.definitionCode != null">
                    AND pi.definition_code = #{filter.definitionCode}
                </if>
                <if test="filter.category != null">
                    AND pi.category = #{filter.category}
                </if>
                <if test="filter.startedFrom != null">
                    AND pi.start_time &gt;= #{filter.startedFrom}
                </if>
                <if test="filter.startedTo != null">
                    AND pi.start_time &lt; #{filter.startedTo}
                </if>
            </where>
            ORDER BY overdueMinutes DESC, pt.due_time ASC
            </script>
            """)
    List<OverdueTaskObservationRow> findOverdueTasks(@Param("filter") MonitorQueryFilter filter);

    @Select("""
            <script>
            SELECT TOP (#{filter.limit})
                pi.id AS instanceId,
                pi.definition_id AS definitionId,
                pi.definition_code AS definitionCode,
                pi.title AS title,
                pi.category AS category,
                pi.initiator_id AS initiatorId,
                pi.status AS status,
                pi.start_time AS startTime,
                pi.end_time AS endTime,
                pi.updated_at AS updatedAt
            FROM proc_instance pi
            <where>
                <if test="filter.tenantId != null">
                    AND pi.tenant_id = #{filter.tenantId}
                </if>
                <if test="filter.definitionId != null">
                    AND pi.definition_id = #{filter.definitionId}
                </if>
                <if test="filter.definitionCode != null">
                    AND pi.definition_code = #{filter.definitionCode}
                </if>
                <if test="filter.category != null">
                    AND pi.category = #{filter.category}
                </if>
                <if test="status != null">
                    AND pi.status = #{status}
                </if>
                <if test="filter.startedFrom != null">
                    AND pi.start_time &gt;= #{filter.startedFrom}
                </if>
                <if test="filter.startedTo != null">
                    AND pi.start_time &lt; #{filter.startedTo}
                </if>
            </where>
            ORDER BY pi.updated_at DESC, pi.start_time DESC
            </script>
            """)
    List<MonitoredProcessInstanceRow> findInstances(
            @Param("filter") MonitorQueryFilter filter,
            @Param("status") String status
    );

    @Select("""
            <script>
            SELECT TOP (#{filter.limit})
                pi.id AS instanceId,
                pi.definition_id AS definitionId,
                pi.definition_code AS definitionCode,
                pi.title AS title,
                pi.category AS category,
                pi.status AS status,
                CASE
                    WHEN pi.status = 'SUSPENDED' THEN 'SUSPENDED'
                    WHEN pt.due_time IS NOT NULL AND pt.due_time &lt; SYSUTCDATETIME() THEN 'OVERDUE_TASK'
                    ELSE 'STALLED_NODE'
                END AS exceptionType,
                MAX(CASE
                    WHEN pi.status = 'SUSPENDED' THEN DATEDIFF_BIG(MINUTE, pi.updated_at, SYSUTCDATETIME())
                    WHEN pt.due_time IS NOT NULL AND pt.due_time &lt; SYSUTCDATETIME()
                        THEN DATEDIFF_BIG(MINUTE, pt.due_time, SYSUTCDATETIME())
                    ELSE DATEDIFF_BIG(MINUTE, pt.created_at, SYSUTCDATETIME())
                END) AS exceptionMinutes,
                SYSUTCDATETIME() AS detectedAt
            FROM proc_instance pi
            LEFT JOIN proc_task pt ON pt.instance_id = pi.id AND pt.status IN ('CREATED', 'CLAIMED')
            <where>
                (pi.status = 'SUSPENDED'
                    OR (pt.due_time IS NOT NULL AND pt.due_time &lt; SYSUTCDATETIME())
                    OR DATEDIFF_BIG(MINUTE, pt.created_at, SYSUTCDATETIME())
                        &gt;= #{filter.stalledThresholdMinutes})
                <if test="filter.tenantId != null">
                    AND pi.tenant_id = #{filter.tenantId}
                </if>
                <if test="filter.definitionId != null">
                    AND pi.definition_id = #{filter.definitionId}
                </if>
                <if test="filter.definitionCode != null">
                    AND pi.definition_code = #{filter.definitionCode}
                </if>
                <if test="filter.category != null">
                    AND pi.category = #{filter.category}
                </if>
            </where>
            GROUP BY pi.id, pi.definition_id, pi.definition_code, pi.title, pi.category, pi.status,
                     CASE
                        WHEN pi.status = 'SUSPENDED' THEN 'SUSPENDED'
                        WHEN pt.due_time IS NOT NULL AND pt.due_time &lt; SYSUTCDATETIME() THEN 'OVERDUE_TASK'
                        ELSE 'STALLED_NODE'
                     END
            ORDER BY exceptionMinutes DESC
            </script>
            """)
    List<ExceptionProcessInstanceRow> findExceptionInstances(@Param("filter") MonitorQueryFilter filter);

    @Select("""
            SELECT
                pt.id AS taskId,
                pt.instance_id AS instanceId,
                pt.node_id AS nodeId,
                pt.node_name AS nodeName,
                pt.node_type AS nodeType,
                pt.assignee_id AS assigneeId,
                pt.status AS taskStatus,
                pt.created_at AS createdAt,
                pt.claim_time AS claimTime,
                pt.completed_time AS completedTime,
                pt.due_time AS dueTime,
                last_action.action_code AS lastActionCode,
                last_action.action_name AS lastActionName,
                last_action.operator_id AS lastOperatorId,
                last_action.created_at AS lastActionAt
            FROM proc_task pt
            INNER JOIN proc_instance pi ON pi.id = pt.instance_id
            OUTER APPLY (
                SELECT TOP (1) action_code, action_name, operator_id, created_at
                FROM proc_task_action pta
                WHERE pta.task_id = pt.id
                ORDER BY pta.created_at DESC
            ) last_action
            WHERE pi.tenant_id = #{tenantId}
              AND pt.instance_id = #{instanceId}
            ORDER BY pt.created_at ASC
            """)
    List<NodeTrailRow> findNodeTrail(@Param("tenantId") UUID tenantId, @Param("instanceId") UUID instanceId);

    @Select("""
            SELECT
                intervention_id AS interventionId,
                instance_id AS instanceId,
                task_id AS taskId,
                action_type AS actionType,
                operator_id AS operatorId,
                target_assignee_id AS targetAssigneeId,
                reason AS reason,
                created_at AS createdAt
            FROM wf_process_intervention
            WHERE tenant_id = #{tenantId}
              AND instance_id = #{instanceId}
            ORDER BY created_at DESC
            """)
    List<ProcessInterventionRow> findInterventions(
            @Param("tenantId") UUID tenantId,
            @Param("instanceId") UUID instanceId
    );

    @Insert("""
            INSERT INTO wf_process_intervention (
                intervention_id,
                tenant_id,
                instance_id,
                task_id,
                action_type,
                operator_id,
                target_assignee_id,
                reason,
                created_at
            ) VALUES (
                #{interventionId},
                #{tenantId},
                #{instanceId},
                #{taskId},
                #{actionType},
                #{operatorId},
                #{targetAssigneeId},
                #{reason},
                SYSUTCDATETIME()
            )
            """)
    void insertIntervention(
            @Param("interventionId") UUID interventionId,
            @Param("tenantId") UUID tenantId,
            @Param("instanceId") UUID instanceId,
            @Param("taskId") UUID taskId,
            @Param("actionType") String actionType,
            @Param("operatorId") UUID operatorId,
            @Param("targetAssigneeId") UUID targetAssigneeId,
            @Param("reason") String reason
    );

    @Select("""
            SELECT
                intervention_id AS interventionId,
                instance_id AS instanceId,
                task_id AS taskId,
                action_type AS actionType,
                operator_id AS operatorId,
                target_assignee_id AS targetAssigneeId,
                reason AS reason,
                created_at AS createdAt
            FROM wf_process_intervention
            WHERE intervention_id = #{interventionId}
            """)
    ProcessInterventionRow findInterventionById(@Param("interventionId") UUID interventionId);

    @Update("""
            UPDATE proc_instance
            SET status = 'SUSPENDED', updated_at = SYSUTCDATETIME()
            WHERE tenant_id = #{tenantId}
              AND id = #{instanceId}
              AND status = 'RUNNING'
            """)
    void suspendInstance(@Param("tenantId") UUID tenantId, @Param("instanceId") UUID instanceId);

    @Update("""
            UPDATE proc_instance
            SET status = 'RUNNING', updated_at = SYSUTCDATETIME()
            WHERE tenant_id = #{tenantId}
              AND id = #{instanceId}
              AND status = 'SUSPENDED'
            """)
    void resumeInstance(@Param("tenantId") UUID tenantId, @Param("instanceId") UUID instanceId);

    @Update("""
            UPDATE proc_instance
            SET status = 'TERMINATED', end_time = SYSUTCDATETIME(), updated_at = SYSUTCDATETIME()
            WHERE tenant_id = #{tenantId}
              AND id = #{instanceId}
              AND status IN ('RUNNING', 'SUSPENDED')
            """)
    void terminateInstance(@Param("tenantId") UUID tenantId, @Param("instanceId") UUID instanceId);

    @Update("""
            UPDATE proc_task
            SET status = 'TERMINATED', completed_time = SYSUTCDATETIME(), updated_at = SYSUTCDATETIME()
            WHERE tenant_id = #{tenantId}
              AND instance_id = #{instanceId}
              AND status IN ('CREATED', 'CLAIMED')
            """)
    void terminateOpenTasks(@Param("tenantId") UUID tenantId, @Param("instanceId") UUID instanceId);

    @Update("""
            UPDATE proc_task
            SET assignee_id = #{targetAssigneeId}, status = 'CLAIMED', updated_at = SYSUTCDATETIME()
            WHERE tenant_id = #{tenantId}
              AND id = #{taskId}
              AND status IN ('CREATED', 'CLAIMED')
            """)
    void reassignTask(
            @Param("tenantId") UUID tenantId,
            @Param("taskId") UUID taskId,
            @Param("targetAssigneeId") UUID targetAssigneeId
    );
}
