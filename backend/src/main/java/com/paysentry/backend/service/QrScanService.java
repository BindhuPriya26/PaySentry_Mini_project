package com.paysentry.backend.service;

import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.paysentry.backend.dto.scan.FraudScanResponse;
import com.paysentry.backend.entity.QrScan;
import com.paysentry.backend.entity.ScanType;
import com.paysentry.backend.entity.User;
import com.paysentry.backend.exception.BadRequestException;
import com.paysentry.backend.repository.QrScanRepository;

@Service
public class QrScanService {

    private final QrScanRepository qrScanRepository;
    private final FraudScoringService fraudScoringService;
    private final ReportService reportService;

    public QrScanService(
        QrScanRepository qrScanRepository,
        FraudScoringService fraudScoringService,
        ReportService reportService
    ) {
        this.qrScanRepository = qrScanRepository;
        this.fraudScoringService = fraudScoringService;
        this.reportService = reportService;
    }

    @Transactional
    public FraudScanResponse scan(User user, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("A QR image file is required.");
        }

        String extractedContent = decodeQr(file);
        RiskAssessment assessment = fraudScoringService.assessQrContent(extractedContent);

        QrScan scan = new QrScan();
        scan.setUser(user);
        scan.setOriginalFileName(file.getOriginalFilename());
        scan.setExtractedContent(extractedContent);
        scan.setExtractedUrl(assessment.getExtractedUrl());
        scan.setFindings(String.join(" | ", assessment.getFindings()));
        scan.setRiskLevel(assessment.getRiskLevel());
        scan.setRiskScore(assessment.getRiskScore());
        scan.setExplanation(assessment.getExplanation());

        QrScan savedScan = qrScanRepository.save(scan);
        reportService.createReport(user, ScanType.QR, savedScan.getId(), "QR Fraud Scan", assessment);

        FraudScanResponse response = new FraudScanResponse();
        response.setScanId(savedScan.getId());
        response.setChannel("QR");
        response.setRiskLevel(savedScan.getRiskLevel().name());
        response.setRiskScore(savedScan.getRiskScore());
        response.setExplanation(savedScan.getExplanation());
        response.setFindings(assessment.getFindings());
        response.setRecommendations(assessment.getRecommendations());
        response.setExtractedContent(savedScan.getExtractedContent());
        response.setExtractedUrl(savedScan.getExtractedUrl());
        response.setCreatedAt(savedScan.getCreatedAt());
        return response;
    }

    private String decodeQr(MultipartFile file) {
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new BadRequestException("Uploaded file is not a readable image.");
            }

            BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = new MultiFormatReader().decode(bitmap);
            return result.getText();
        } catch (NotFoundException exception) {
            throw new BadRequestException("No QR code could be detected in the uploaded image.");
        } catch (BadRequestException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BadRequestException("Unable to process the QR image.");
        }
    }
}
