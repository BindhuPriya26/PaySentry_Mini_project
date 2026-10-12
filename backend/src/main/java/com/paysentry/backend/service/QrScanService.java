package com.paysentry.backend.service;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.UUID;

import javax.imageio.ImageReader;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

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
import com.paysentry.backend.entity.BankingEventType;
import com.paysentry.backend.entity.ScanType;
import com.paysentry.backend.entity.User;
import com.paysentry.backend.exception.BadRequestException;
import com.paysentry.backend.monitoring.BankAccountService;
import com.paysentry.backend.monitoring.RiskEngineService;
import com.paysentry.backend.monitoring.RiskOutcome;
import com.paysentry.backend.repository.QrScanRepository;

@Service
public class QrScanService {

    private static final long MAX_QR_IMAGE_PIXELS = 12_000_000L;

    private final QrScanRepository qrScanRepository;
    private final FraudScoringService fraudScoringService;
    private final ReportService reportService;
    private final BankAccountService bankAccountService;
    private final RiskEngineService riskEngineService;
    private final SensitiveContentService sensitiveContentService;

    public QrScanService(
        QrScanRepository qrScanRepository,
        FraudScoringService fraudScoringService,
        ReportService reportService,
        BankAccountService bankAccountService,
        RiskEngineService riskEngineService,
        SensitiveContentService sensitiveContentService
    ) {
        this.qrScanRepository = qrScanRepository;
        this.fraudScoringService = fraudScoringService;
        this.reportService = reportService;
        this.bankAccountService = bankAccountService;
        this.riskEngineService = riskEngineService;
        this.sensitiveContentService = sensitiveContentService;
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
        scan.setOriginalFileName("QR image analyzed (filename not retained)");
        scan.setExtractedContent("QR content not retained. fingerprint=" + sensitiveContentService.fingerprint(extractedContent));
        scan.setExtractedUrl(assessment.getExtractedUrl() == null ? null : sensitiveContentService.safeUrlEvidence(assessment.getExtractedUrl()));
        scan.setFindings(String.join(" | ", assessment.getFindings()));
        scan.setRiskLevel(assessment.getRiskLevel());
        scan.setRiskScore(assessment.getRiskScore());
        scan.setExplanation(assessment.getExplanation());

        QrScan savedScan = qrScanRepository.save(scan);
        RiskOutcome unifiedRisk = riskEngineService.recordSignal(
            user,
            bankAccountService.getForUser(user),
            BankingEventType.QR,
            "qr-upload-" + UUID.randomUUID(),
            assessment.getRiskScore(),
            assessment.getFindings(),
            "contentFingerprint=" + sensitiveContentService.fingerprint(extractedContent),
            LocalDateTime.now()
        );
        reportService.createUnifiedReport(user, ScanType.QR, savedScan.getId(), "QR Fraud Scan", unifiedRisk);

        FraudScanResponse response = new FraudScanResponse();
        response.setScanId(savedScan.getId());
        response.setChannel("QR");
        response.setRiskLevel(unifiedRisk.getRiskLevel().name());
        response.setRiskScore(unifiedRisk.getScore());
        response.setExplanation(savedScan.getExplanation());
        response.setFindings(unifiedRisk.getReasons());
        response.setRecommendations(assessment.getRecommendations());
        response.setExtractedContent(extractedContent);
        response.setExtractedUrl(assessment.getExtractedUrl());
        response.setCreatedAt(savedScan.getCreatedAt());
        return response;
    }

    private String decodeQr(MultipartFile file) {
        try (InputStream input = file.getInputStream();
             ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
            if (imageInput == null) {
                throw new BadRequestException("Uploaded file is not a readable image.");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new BadRequestException("Uploaded file is not a readable image.");
            }

            ImageReader reader = readers.next();
            BufferedImage image;
            try {
                reader.setInput(imageInput, true, true);
                validateImageDimensions(reader.getWidth(0), reader.getHeight(0));
                image = reader.read(0);
            } finally {
                reader.dispose();
            }
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

    static void validateImageDimensions(int width, int height) {
        if (width <= 0 || height <= 0 || (long) width * height > MAX_QR_IMAGE_PIXELS) {
            throw new BadRequestException("QR image dimensions exceed the supported limit.");
        }
    }
}
