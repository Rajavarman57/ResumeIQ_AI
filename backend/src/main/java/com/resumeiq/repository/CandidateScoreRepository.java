package com.resumeiq.repository;
import com.resumeiq.model.CandidateScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
public interface CandidateScoreRepository extends JpaRepository<CandidateScore, Long> {
    List<CandidateScore> findByJobRoleIdOrderByMatchScoreDesc(Long jobId);
    List<CandidateScore> findByResumeId(Long resumeId);
    Optional<CandidateScore> findByResumeIdAndJobRoleId(Long resumeId, Long jobId);
    @Query("SELECT AVG(cs.matchScore) FROM CandidateScore cs WHERE cs.jobRole.id = :jobId")
    Double avgScoreByJob(Long jobId);
    List<CandidateScore> findByJobRoleCreatedById(Long userId);
}
