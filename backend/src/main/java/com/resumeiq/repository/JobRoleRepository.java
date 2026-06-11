package com.resumeiq.repository;
import com.resumeiq.model.JobRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface JobRoleRepository extends JpaRepository<JobRole, Long> {
    List<JobRole> findByStatus(JobRole.Status status);
    List<JobRole> findByStatusNot(JobRole.Status status);
    List<JobRole> findByCreatedById(Long userId);
    long countByCreatedById(Long userId);
    long countByCreatedByIdAndStatus(Long userId, JobRole.Status status);
}
