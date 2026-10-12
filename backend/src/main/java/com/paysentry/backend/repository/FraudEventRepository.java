package com.paysentry.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paysentry.backend.entity.FraudEvent;
import com.paysentry.backend.entity.User;

public interface FraudEventRepository extends JpaRepository<FraudEvent, Long> {

    Optional<FraudEvent> findByExternalEventId(String externalEventId);

    List<FraudEvent> findByUserAndOccurredAtBetweenOrderByOccurredAtDesc(
        User user,
        LocalDateTime occurredAfter,
        LocalDateTime occurredBefore
    );

    List<FraudEvent> findTop10ByUserOrderByOccurredAtDesc(User user);

    long countByRiskLevelIn(List<com.paysentry.backend.entity.UnifiedRiskLevel> levels);
}
