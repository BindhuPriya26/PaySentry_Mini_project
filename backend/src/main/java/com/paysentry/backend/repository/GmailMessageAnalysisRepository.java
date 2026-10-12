package com.paysentry.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paysentry.backend.entity.GmailConnection;
import com.paysentry.backend.entity.GmailMessageAnalysis;

public interface GmailMessageAnalysisRepository extends JpaRepository<GmailMessageAnalysis, Long> {
    boolean existsByConnectionAndGmailMessageId(GmailConnection connection, String gmailMessageId);
    long deleteByConnection(GmailConnection connection);
}
