package com.hjo2oa.process.monitor.infrastructure;

import com.hjo2oa.process.monitor.domain.MonitorQueryFilter;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
