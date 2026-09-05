package com.paysentry.backend.service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.paysentry.backend.dto.report.ReportResponse;
import com.paysentry.backend.entity.Report;
import com.paysentry.backend.entity.ScanType;
import com.paysentry.backend.entity.User;
import com.paysentry.backend.exception.ResourceNotFoundException;
import com.paysentry.backend.repository.ReportRepository;

@Service
public class ReportService {

    private static final DateTimeFormatter PDF_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public Report createReport(User user, ScanType scanType, Long referenceId, String title, RiskAssessment assessment) {
        Report report = new Report();
        report.setUser(user);
        report.setScanType(scanType);
        report.setReferenceId(referenceId);
        report.setTitle(title);
        report.setSummary(assessment.getExplanation());
        report.setRecommendation(String.join(" ", assessment.getRecommendations()));
        report.setRiskLevel(assessment.getRiskLevel());
        report.setRiskScore(assessment.getRiskScore());
        return reportRepository.save(report);
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> listReportsForUser(User user) {
        return reportRepository.findByUserOrderByCreatedAtDesc(user)
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> listAllReports() {
        return reportRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public byte[] generatePdfForUser(User user, Long reportId) {
        Report report = reportRepository.findByIdAndUser(reportId, user)
            .orElseThrow(() -> new ResourceNotFoundException("Report not found."));
        return createPdf(report);
    }

    public ReportResponse toResponse(Report report) {
        ReportResponse response = new ReportResponse();
        response.setId(report.getId());
        response.setOwnerName(report.getUser().getFullName());
        response.setOwnerEmail(report.getUser().getEmail());
        response.setScanType(report.getScanType().name());
        response.setTitle(report.getTitle());
        response.setSummary(report.getSummary());
        response.setRecommendation(report.getRecommendation());
        response.setRiskLevel(report.getRiskLevel().name());
        response.setRiskScore(report.getRiskScore());
        response.setCreatedAt(report.getCreatedAt());
        return response;
    }

    private byte[] createPdf(Report report) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            document.open();

            document.add(new Paragraph("PaySentry Fraud Report", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Report ID: " + report.getId()));
            document.add(new Paragraph("Owner: " + report.getUser().getFullName() + " (" + report.getUser().getEmail() + ")"));
            document.add(new Paragraph("Channel: " + report.getScanType().name()));
            document.add(new Paragraph("Risk Level: " + report.getRiskLevel().name()));
            document.add(new Paragraph("Risk Score: " + report.getRiskScore()));
            document.add(new Paragraph("Created At: " + report.getCreatedAt().format(PDF_DATE_FORMATTER)));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Summary", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
            document.add(new Paragraph(report.getSummary()));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Recommendation", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
            document.add(new Paragraph(report.getRecommendation()));

            document.close();
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate PDF report.", exception);
        }
    }
}
