package com.paysentry.backend.service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paysentry.backend.dto.admin.SystemStatsResponse;
import com.paysentry.backend.dto.admin.UserSummaryResponse;
import com.paysentry.backend.dto.report.ReportResponse;
import com.paysentry.backend.entity.Report;
import com.paysentry.backend.entity.RiskLevel;
import com.paysentry.backend.entity.User;
import com.paysentry.backend.exception.ResourceNotFoundException;
import com.paysentry.backend.repository.QrScanRepository;
import com.paysentry.backend.repository.ReportRepository;
import com.paysentry.backend.repository.SmsScanRepository;
import com.paysentry.backend.repository.TransactionRecordRepository;
import com.paysentry.backend.repository.UrlScanRepository;
import com.paysentry.backend.repository.UserRepository;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final SmsScanRepository smsScanRepository;
    private final QrScanRepository qrScanRepository;
    private final UrlScanRepository urlScanRepository;
    private final TransactionRecordRepository transactionRecordRepository;
    private final ReportService reportService;

    public AdminService(
        UserRepository userRepository,
        ReportRepository reportRepository,
        SmsScanRepository smsScanRepository,
        QrScanRepository qrScanRepository,
        UrlScanRepository urlScanRepository,
        TransactionRecordRepository transactionRecordRepository,
        ReportService reportService
    ) {
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
        this.smsScanRepository = smsScanRepository;
        this.qrScanRepository = qrScanRepository;
        this.urlScanRepository = urlScanRepository;
        this.transactionRecordRepository = transactionRecordRepository;
        this.reportService = reportService;
    }

    public List<UserSummaryResponse> listUsers() {
        return userRepository.findAll()
            .stream()
            .sorted(Comparator.comparing(User::getCreatedAt).reversed())
            .map(this::toUserSummary)
            .collect(Collectors.toList());
    }

    @Transactional
    public UserSummaryResponse updateUserStatus(Long userId, boolean active) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        user.setActive(active);
        return toUserSummary(userRepository.save(user));
    }

    public List<ReportResponse> listReports() {
        return reportRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .map(reportService::toResponse)
            .collect(Collectors.toList());
    }

    public SystemStatsResponse getSystemStats() {
        SystemStatsResponse response = new SystemStatsResponse();
        response.setTotalUsers(userRepository.count());
        response.setActiveUsers(userRepository.countByActiveTrue());
        response.setTotalReports(reportRepository.count());
        response.setHighRiskReports(reportRepository.countByRiskLevelIn(List.of(RiskLevel.HIGH_RISK)));
        response.setTotalSmsScans(smsScanRepository.count());
        response.setTotalQrScans(qrScanRepository.count());
        response.setTotalUrlScans(urlScanRepository.count());
        response.setTotalTransactions(transactionRecordRepository.count());
        return response;
    }

    private UserSummaryResponse toUserSummary(User user) {
        UserSummaryResponse response = new UserSummaryResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setActive(user.isActive());
        response.setLastLoginAt(user.getLastLoginAt());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}
