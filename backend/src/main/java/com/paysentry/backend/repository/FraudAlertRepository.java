package com.paysentry.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paysentry.backend.entity.FraudAlert;
import com.paysentry.backend.entity.User;

public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {

    List<FraudAlert> findTop20ByUserOrderByCreatedAtDesc(User user);

    long countByUser(User user);
}
