package com.resumeiq.repository;
import com.resumeiq.model.CandidateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface CandidateStatusRepository extends JpaRepository<CandidateStatus, Long> {
    List<CandidateStatus> findByJobRoleId(Long jobId);
    List<CandidateStatus> findByResumeId(Long resumeId);
    Optional<CandidateStatus> findByResumeIdAndJobRoleId(Long resumeId, Long jobId);
    long countByStatus(CandidateStatus.Status status);
    long countByJobRoleId(Long jobId);
    long countByStatusAndJobRoleCreatedById(CandidateStatus.Status status, Long userId);
    long countByJobRoleCreatedById(Long userId);
}
