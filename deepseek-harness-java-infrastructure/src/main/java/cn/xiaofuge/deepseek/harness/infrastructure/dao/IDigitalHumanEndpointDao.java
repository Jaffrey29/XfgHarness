package cn.xiaofuge.deepseek.harness.infrastructure.dao;

import cn.xiaofuge.deepseek.harness.infrastructure.dao.po.DigitalHumanEndpointPO;
import java.util.Optional;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 数字人端点持久化。
 */
@Mapper
public interface IDigitalHumanEndpointDao {

    @Insert("""
            INSERT INTO digital_human_endpoint (id, digital_human_id, endpoint_type, base_url, credential_ref, protocol_version, agent_card, health_state, last_checked_at, latency_ms)
            VALUES (#{id}, #{digitalHumanId}, #{endpointType}, #{baseUrl}, #{credentialRef}, #{protocolVersion}, #{agentCard}, #{healthState}, #{lastCheckedAt}, #{latencyMs})
            """)
    void insert(DigitalHumanEndpointPO po);

    @Update("""
            UPDATE digital_human_endpoint
            SET endpoint_type = #{endpointType}, base_url = #{baseUrl}, credential_ref = #{credentialRef},
                protocol_version = #{protocolVersion}, agent_card = #{agentCard}, health_state = #{healthState},
                last_checked_at = #{lastCheckedAt}, latency_ms = #{latencyMs}
            WHERE digital_human_id = #{digitalHumanId}
            """)
    int updateByHumanId(DigitalHumanEndpointPO po);

    @Update("""
            UPDATE digital_human_endpoint
            SET health_state = #{healthState}, last_checked_at = #{lastCheckedAt}, latency_ms = #{latencyMs}
            WHERE digital_human_id = #{digitalHumanId}
            """)
    int updateHealth(@Param("digitalHumanId") String digitalHumanId,
                     @Param("healthState") String healthState,
                     @Param("lastCheckedAt") java.time.Instant lastCheckedAt,
                     @Param("latencyMs") Integer latencyMs);

    @Select("""
            SELECT id, digital_human_id, endpoint_type, base_url, credential_ref, protocol_version, agent_card, health_state, last_checked_at, latency_ms
            FROM digital_human_endpoint WHERE digital_human_id = #{digitalHumanId}
            """)
    Optional<DigitalHumanEndpointPO> findByHumanId(@Param("digitalHumanId") String digitalHumanId);

    @Delete("DELETE FROM digital_human_endpoint WHERE digital_human_id = #{digitalHumanId}")
    int deleteByHumanId(@Param("digitalHumanId") String digitalHumanId);
}
