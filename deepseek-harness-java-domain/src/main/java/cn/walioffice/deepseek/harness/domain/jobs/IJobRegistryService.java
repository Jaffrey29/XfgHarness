package cn.walioffice.deepseek.harness.domain.jobs;

import cn.walioffice.deepseek.harness.domain.jobs.model.entity.JobStart;
import cn.walioffice.deepseek.harness.domain.jobs.model.valobj.JobSnapshot;
import java.util.List;
import java.util.Optional;

/**
 * Background-job capability seam. Start, read, list, and cancel jobs.
 *
 * @author 小傅哥
 * @website bugstack.cn
 */
public interface IJobRegistryService {

    JobSnapshot start(JobStart start);

    Optional<JobSnapshot> read(String id, String requestingSessionId);

    List<JobSnapshot> list(String requestingSessionId);

    boolean cancel(String id, String requestingSessionId, String reason);
}
