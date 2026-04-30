package com.hjo2oa.portal.aggregation.api.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PortalCardSnapshotMapper extends BaseMapper<PortalCardSnapshotEntity> {

    String SNAPSHOT_COLUMNS = """
            snapshot_id, tenant_id, person_id, assignment_id, position_id, scene_type, card_type,
            state, data_json, message, refreshed_at
            """;

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT
            """ + SNAPSHOT_COLUMNS + """
            FROM portal_card_snapshot
            WHERE snapshot_id = #{snapshotId}
            """)
    PortalCardSnapshotEntity selectBySnapshotId(@Param("snapshotId") String snapshotId);

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT
            """ + SNAPSHOT_COLUMNS + """
            FROM portal_card_snapshot
            ORDER BY refreshed_at DESC
            """)
    List<PortalCardSnapshotEntity> selectAllSnapshots();

    @InterceptorIgnore(tenantLine = "true")
    @Update("""
            UPDATE portal_card_snapshot
            SET tenant_id = #{tenantId},
                person_id = #{personId},
                assignment_id = #{assignmentId},
                position_id = #{positionId},
                scene_type = #{sceneType},
                card_type = #{cardType},
                state = #{state},
                data_json = #{dataJson},
                message = #{message},
                refreshed_at = #{refreshedAt}
            WHERE snapshot_id = #{snapshotId}
            """)
    int updateSnapshot(PortalCardSnapshotEntity entity);
}
