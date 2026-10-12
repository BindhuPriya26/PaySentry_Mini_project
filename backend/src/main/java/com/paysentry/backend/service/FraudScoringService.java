package com.paysentry.backend.service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.paysentry.backend.entity.RiskLevel;
import com.paysentry.backend.exception.BadRequestException;

@Service
public class FraudScoringService {

    private static final List<String> SUSPICIOUS_URL_KEYWORDS = Arrays.asList(
        "login", "verify", "secure", "update", "bank", "wallet", "otp", "urgent", "bonus", "reward"
    );

    private static final List<String> HOSTNAME_LURE_KEYWORDS = Arrays.asList(
        "login", "secure", "verify", "verification", "update", "reset", "unlock", "wallet", "otp", "kyc"
    );

    private static final List<String> SHORTENER_DOMAINS = Arrays.asList(
        "bit.ly", "tinyurl.com", "cutt.ly", "rb.gy", "is.gd", "goo.gl"
    );

    private static final List<String> SUSPICIOUS_TLDS = Arrays.asList(
        ".xyz", ".top", ".click", ".link", ".live", ".zip"
    );

    public RiskAssessment assessSms(String messageContent) {
        String message = messageContent.trim();
        String lower = message.toLowerCase(Locale.ROOT);
        Map<String, Integer> keywordWeights = new LinkedHashMap<>();
        keywordWeights.put("otp", 18);
        keywordWeights.put("account blocked", 20);
        keywordWeights.put("blocked", 14);
        keywordWeights.put("click here", 18);
        keywordWeights.put("verify", 14);
        keywordWeights.put("urgent", 12);
        keywordWeights.put("kyc", 14);
        keywordWeights.put("suspended", 16);
        keywordWeights.put("reward", 10);
        keywordWeights.put("refund", 10);

        int score = 0;
        List<String> findings = new ArrayList<>();
        List<String> matchedKeywords = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : keywordWeights.entrySet()) {
            if (lower.contains(entry.getKey())) {
                matchedKeywords.add(entry.getKey());
                score += entry.getValue();
            }
        }

        if (!matchedKeywords.isEmpty()) {
            findings.add("Matched suspicious keywords: " + String.join(", ", matchedKeywords) + ".");
        }
        if (containsLink(lower)) {
            score += 18;
            findings.add("Contains a clickable link.");
        }
        if (containsShortener(lower)) {
            score += 15;
            findings.add("Uses a shortened link, which can hide the real destination.");
        }
        if (message.chars().filter(character -> character == '!').count() >= 2) {
            score += 8;
            findings.add("Uses aggressive urgency markers.");
        }
        if (lower.contains("call now") || lower.contains("reply now")) {
            score += 10;
            findings.add("Pushes an immediate response.");
        }

        score = Math.min(score, 100);
        if (findings.isEmpty()) {
            findings.add("No common fraud indicators were detected.");
        }

        RiskLevel riskLevel = determineRiskLevel(score);
        String explanation = buildExplanation("SMS scan", riskLevel, score, findings);
        return new RiskAssessment(
            riskLevel,
            score,
            explanation,
            findings,
            buildRecommendations(riskLevel),
            null
        );
    }

    public RiskAssessment assessUrl(String rawUrl) {
        String input = rawUrl.trim();
        if (input.isEmpty()) {
            throw new BadRequestException("A URL is required.");
        }
        String normalized = ensureUrlHasScheme(input);
        String lower = normalized.toLowerCase(Locale.ROOT);
        List<String> findings = new ArrayList<>();
        int score = 0;

        try {
            URI uri = new URI(normalized);
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                throw new BadRequestException("Only HTTP and HTTPS URLs can be analyzed.");
            }
            String host = uri.getHost();

            if (!lower.startsWith("https://")) {
                score += 20;
                findings.add("URL does not use HTTPS.");
            }
            if (host == null || host.isBlank()) {
                score += 30;
                findings.add("Domain could not be validated.");
            } else {
                String hostLower = host.toLowerCase(Locale.ROOT);
                String pathAndQuery = buildPathAndQuery(uri);

                if (!isValidDomain(hostLower)) {
                    score += 20;
                    findings.add("Domain structure looks unusual.");
                }
                if (isIpAddress(hostLower)) {
                    score += 25;
                    findings.add("URL uses an IP address instead of a named domain.");
                }
                if (hostLower.contains("xn--")) {
                    score += 20;
                    findings.add("Domain contains punycode, which can mask look-alike phishing domains.");
                }
                if (containsShortener(hostLower)) {
                    score += 25;
                    findings.add("Domain uses a URL shortener that can conceal the target site.");
                }
                if (hasSuspiciousTld(hostLower)) {
                    score += 18;
                    findings.add("Domain uses a high-risk top-level domain.");
                }

                List<String> matchedHostnameKeywords = HOSTNAME_LURE_KEYWORDS.stream()
                    .filter(hostLower::contains)
                    .collect(Collectors.toList());

                if (!matchedHostnameKeywords.isEmpty()) {
                    score += matchedHostnameKeywords.size() * 10;
                    findings.add(
                        "Hostname contains phishing lure terms: " + String.join(", ", matchedHostnameKeywords) + "."
                    );

                    if (matchedHostnameKeywords.size() >= 2) {
                        score += 15;
                        findings.add("Hostname combines multiple security-related lure terms.");
                    }
                }

                if (hostLower.chars().filter(character -> character == '-').count() >= 2) {
                    score += 10;
                    findings.add("Hostname uses multiple hyphen-separated terms, which is common in fake domains.");
                } else if (!matchedHostnameKeywords.isEmpty() && hostLower.contains("-")) {
                    score += 6;
                    findings.add("Hostname mixes lure terms with hyphenated wording.");
                }

                if (hasExcessiveSubdomains(hostLower)) {
                    score += 10;
                    findings.add("URL uses many subdomains, which can imitate trusted brands.");
                }

                if (!pathAndQuery.isBlank()) {
                    List<String> matchedPathKeywords = SUSPICIOUS_URL_KEYWORDS.stream()
                        .filter(pathAndQuery::contains)
                        .collect(Collectors.toList());

                    if (!matchedPathKeywords.isEmpty()) {
                        score += matchedPathKeywords.size() * 4;
                        findings.add(
                            "Path or query contains suspicious terms: " + String.join(", ", matchedPathKeywords) + "."
                        );
                    }
                }
            }

            List<String> matchedKeywords = new ArrayList<>();
            for (String keyword : SUSPICIOUS_URL_KEYWORDS) {
                if (lower.contains(keyword)) {
                    matchedKeywords.add(keyword);
                }
            }
            if (!matchedKeywords.isEmpty()) {
                findings.add("Suspicious URL keywords detected: " + String.join(", ", matchedKeywords) + ".");

                if (matchedKeywords.size() >= 3) {
                    score += 12;
                    findings.add("Multiple phishing keywords appear across the URL.");
                }
            }

            if (uri.getUserInfo() != null) {
                score += 20;
                findings.add("URL contains embedded user information.");
            }
            if (uri.getPort() > 0 && uri.getPort() != 80 && uri.getPort() != 443) {
                score += 10;
                findings.add("URL uses an uncommon network port.");
            }
        } catch (URISyntaxException exception) {
            score = 85;
            findings.clear();
            findings.add("URL format is invalid or intentionally obfuscated.");
        }

        score = Math.min(score, 100);
        if (findings.isEmpty()) {
            findings.add("No major phishing indicators were detected.");
        }

        RiskLevel riskLevel = determineRiskLevel(score);
        String explanation = buildExplanation("URL scan", riskLevel, score, findings);
        return new RiskAssessment(
            riskLevel,
            score,
            explanation,
            findings,
            buildRecommendations(riskLevel),
            normalized
        );
    }

    /**
     * Scores sender and message metadata in memory. The caller must not persist an email body,
     * attachment body, access token, or refresh token as analysis evidence.
     */
    public RiskAssessment assessEmail(
        String sender,
        String subject,
        String body,
        List<String> urls,
        boolean hasSuspiciousAttachment
    ) {
        String normalizedSender = sender == null ? "" : sender.trim().toLowerCase(Locale.ROOT);
        String normalizedSubject = subject == null ? "" : subject.trim();
        String normalizedBody = body == null ? "" : body.trim();
        String combined = normalizedSubject + "\n" + normalizedBody;
        int score = 0;
        List<String> findings = new ArrayList<>();

        if (normalizedSender.isEmpty() || !normalizedSender.contains("@")) {
            score += 15;
            findings.add("Sender address is missing or malformed.");
        }
        if (normalizedSender.contains("xn--") || normalizedSender.matches(".*[a-z0-9]{2,}[-_.][a-z0-9]{2,}.*@.*")) {
            score += 15;
            findings.add("Sender address has look-alike or unusually structured identifiers.");
        }

        RiskAssessment messageAssessment = assessSms(combined);
        int keywordSignal = Math.min(35, messageAssessment.getRiskScore());
        if (keywordSignal >= 18) {
            score += keywordSignal;
            findings.add("Email contains urgency, credential, OTP, KYC, or account-blocking indicators.");
        }

        int suspiciousUrls = 0;
        for (String url : urls == null ? java.util.Collections.<String>emptyList() : urls) {
            try {
                RiskAssessment urlAssessment = assessUrl(url);
                if (urlAssessment.getRiskScore() >= 35) {
                    suspiciousUrls++;
                }
            } catch (BadRequestException ignored) {
                suspiciousUrls++;
            }
        }
        if (suspiciousUrls > 0) {
            score += Math.min(35, 18 + (suspiciousUrls - 1) * 8);
            findings.add("Email contains " + suspiciousUrls + " suspicious or malformed link(s).");
        }
        if (hasSuspiciousAttachment) {
            score += 20;
            findings.add("Email includes an attachment type commonly used to deliver malware or phishing content.");
        }

        score = Math.min(score, 100);
        if (findings.isEmpty()) {
            findings.add("No strong phishing indicators were detected in the authorized email metadata and content.");
        }
        RiskLevel riskLevel = determineRiskLevel(score);
        return new RiskAssessment(
            riskLevel,
            score,
            buildExplanation("Gmail message analysis", riskLevel, score, findings),
            findings,
            buildRecommendations(riskLevel),
            null
        );
    }

    public RiskAssessment assessQrContent(String qrContent) {
        String content = qrContent == null ? "" : qrContent.trim();
        String lower = content.toLowerCase(Locale.ROOT);

        if (looksLikeUrl(content)) {
            RiskAssessment urlAssessment = assessUrl(content);
            List<String> findings = new ArrayList<>();
            findings.add("QR code points to an external destination.");
            findings.addAll(urlAssessment.getFindings());
            return new RiskAssessment(
                urlAssessment.getRiskLevel(),
                urlAssessment.getRiskScore(),
                buildExplanation("QR scan", urlAssessment.getRiskLevel(), urlAssessment.getRiskScore(), findings),
                findings,
                urlAssessment.getRecommendations(),
                urlAssessment.getExtractedUrl()
            );
        }

        List<String> findings = new ArrayList<>();
        int score = 10;
        String extractedUrl = null;

        if (lower.startsWith("upi://pay") || lower.contains("pa=") || lower.contains("pn=")) {
            score = 45;
            findings.add("QR code encodes a direct payment request.");
            if (lower.contains("am=") || lower.contains("tn=")) {
                score += 10;
                findings.add("Payment QR contains transaction parameters that should be verified carefully.");
            }
        } else if (lower.contains("http") || lower.contains("www.")) {
            score = 65;
            findings.add("QR content contains an unstructured web reference.");
        } else {
            findings.add("QR content is plain text and does not expose a direct web destination.");
        }

        score = Math.min(score, 100);
        RiskLevel riskLevel = determineRiskLevel(score);
        String explanation = buildExplanation("QR scan", riskLevel, score, findings);
        return new RiskAssessment(
            riskLevel,
            score,
            explanation,
            findings,
            buildRecommendations(riskLevel),
            extractedUrl
        );
    }

    public RiskAssessment assessTransaction(
        BigDecimal amount,
        String receiver,
        String location,
        String device,
        LocalDateTime transactionTime,
        boolean newDevice,
        boolean newLocation,
        long recentTransactionCount
    ) {
        int score = 0;
        List<String> findings = new ArrayList<>();
        String receiverLower = receiver.toLowerCase(Locale.ROOT);
        String deviceLower = device.toLowerCase(Locale.ROOT);

        if (amount.compareTo(new BigDecimal("100000")) >= 0) {
            score += 35;
            findings.add("Transaction amount is unusually high.");
        } else if (amount.compareTo(new BigDecimal("50000")) >= 0) {
            score += 25;
            findings.add("Transaction amount is above the medium-risk threshold.");
        } else if (amount.compareTo(new BigDecimal("20000")) >= 0) {
            score += 15;
            findings.add("Transaction amount is higher than routine transfers.");
        }

        if (newDevice) {
            score += 25;
            findings.add("Transaction originates from an unrecognized device.");
        }
        if (newLocation) {
            score += 20;
            findings.add("Transaction comes from a new location.");
        }
        if (transactionTime.getHour() < 6 || transactionTime.getHour() >= 23) {
            score += 15;
            findings.add("Transaction is occurring at an unusual hour.");
        }
        if (recentTransactionCount >= 3) {
            score += 20;
            findings.add("Multiple transactions were initiated within the last hour.");
        }
        if (receiverLower.contains("crypto") || receiverLower.contains("wallet")
            || receiverLower.contains("gift") || receiverLower.contains("cash")) {
            score += 10;
            findings.add("Receiver name matches a higher-risk transfer pattern.");
        }
        if (deviceLower.contains("emulator") || deviceLower.contains("virtual")) {
            score += 10;
            findings.add("Device name suggests a virtual or emulated environment.");
        }

        score = Math.min(score, 100);
        if (findings.isEmpty()) {
            findings.add("Transaction aligns with known behavior patterns.");
        }

        RiskLevel riskLevel = determineRiskLevel(score);
        String explanation = buildExplanation("Transaction analysis", riskLevel, score, findings);
        return new RiskAssessment(
            riskLevel,
            score,
            explanation,
            findings,
            buildRecommendations(riskLevel),
            null
        );
    }

    private RiskLevel determineRiskLevel(int score) {
        if (score >= 70) {
            return RiskLevel.HIGH_RISK;
        }
        if (score >= 35) {
            return RiskLevel.MEDIUM_RISK;
        }
        return RiskLevel.SAFE;
    }

    private List<String> buildRecommendations(RiskLevel riskLevel) {
        if (riskLevel == RiskLevel.HIGH_RISK) {
            return Arrays.asList(
                "Do not proceed until the source is verified through an official channel.",
                "Avoid sharing passwords, OTPs, PINs, or card details.",
                "Flag the activity for manual review or report it to the bank immediately."
            );
        }
        if (riskLevel == RiskLevel.MEDIUM_RISK) {
            return Arrays.asList(
                "Verify the sender, website, or receiver independently before proceeding.",
                "Use official banking apps or known contact numbers instead of embedded links.",
                "Monitor account activity closely for the next 24 hours."
            );
        }
        return Arrays.asList(
            "No major risk was detected, but continue following standard banking safety practices.",
            "Avoid sharing OTPs or credentials even when a message or site looks legitimate."
        );
    }

    private String buildExplanation(String channel, RiskLevel riskLevel, int score, List<String> findings) {
        String basis = findings.isEmpty() ? "No additional details were detected." : findings.get(0);
        return channel + " classified the activity as " + riskLevel.name()
            + " with a risk score of " + score + ". Primary signal: " + basis;
    }

    private String ensureUrlHasScheme(String url) {
        if (url.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
            return url;
        }
        return "http://" + url;
    }

    private boolean containsLink(String content) {
        return content.contains("http://") || content.contains("https://") || content.contains("www.");
    }

    private boolean containsShortener(String content) {
        return SHORTENER_DOMAINS.stream().anyMatch(content::contains);
    }

    private boolean looksLikeUrl(String value) {
        return value.matches("(?i)^(https?://)?[a-z0-9.-]+\\.[a-z]{2,}.*$");
    }

    private boolean isValidDomain(String host) {
        return host.matches("^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }

    private boolean isIpAddress(String host) {
        return host.matches("^\\d{1,3}(\\.\\d{1,3}){3}$");
    }

    private boolean hasSuspiciousTld(String host) {
        return SUSPICIOUS_TLDS.stream().anyMatch(host::endsWith);
    }

    private boolean hasExcessiveSubdomains(String host) {
        return host.split("\\.").length >= 4;
    }

    private String buildPathAndQuery(URI uri) {
        StringBuilder value = new StringBuilder();

        if (uri.getPath() != null) {
            value.append(uri.getPath().toLowerCase(Locale.ROOT));
        }
        if (uri.getQuery() != null) {
            value.append('?').append(uri.getQuery().toLowerCase(Locale.ROOT));
        }

        return value.toString();
    }
}
