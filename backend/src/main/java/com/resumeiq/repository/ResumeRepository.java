package com.resumeiq.repository;
import com.resumeiq.model.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ResumeRepository extends JpaRepository<Resume, Long> {
    List<Resume> findByUploadedById(Long userId);
    long countByUploadedById(Long userId);
}
