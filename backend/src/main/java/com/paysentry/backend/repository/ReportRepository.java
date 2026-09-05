package com.paysentry.backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paysentry.backend.entity.Report;
import com.paysentry.backend.entity.RiskLevel;
import com.paysentry.backend.entity.User;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByUserOrderByCreatedAtDesc(User user);

    List<Report> findTop10ByUserOrderByCreatedAtDesc(User user);

    List<Report> findAllByOrderByCreatedAtDesc();

    Optional<Report> findByIdAndUser(Long id, User user);

    long countByUserAndRiskLevelIn(User user, Collection<RiskLevel> riskLevels);

    long countByRiskLevelIn(Collection<RiskLevel> riskLevels);
}
