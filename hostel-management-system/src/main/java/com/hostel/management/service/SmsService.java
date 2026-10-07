package com.hostel.management.service;

import com.hostel.management.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public interface SmsService {
    void sendOtp(String mobileNumber, String otp, String purpose);
    String getLatestDevOtp(String mobileNumber);
    boolean isRealMode();
}

@Service
@Slf4j
class ProviderIndependentSmsService implements SmsService {

    private static final Pattern INDIAN_MOBILE_PATTERN = Pattern.compile("^[6-9]\\d{9}$");

    @Value("${app.otp.sms.mode:simulation}")
    private String smsMode;

    @Value("${app.sms.provider:twilio}")
    private String smsProvider;

    // Twilio credentials
    @Value("${app.sms.twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${app.sms.twilio.auth-token:}")
    private String twilioAuthToken;

    @Value("${app.sms.twilio.phone-number:}")
    private String twilioPhoneNumber;

    // Fast2SMS credentials
    @Value("${app.sms.fast2sms.api-key:}")
    private String fast2smsApiKey;

    @Value("${app.sms.fast2sms.route:otp}")
    private String fast2smsRoute;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final Map<String, String> latestDevOtps = new ConcurrentHashMap<>();

    @Override
    public boolean isRealMode() {
        return "real".equalsIgnoreCase(smsMode != null ? smsMode.trim() : "");
    }

    @Override
    public void sendOtp(String mobileNumber, String otp, String purpose) {
        String cleanNumber = normalizeMobileNumber(mobileNumber);

        if (isRealMode()) {
            sendRealSms(cleanNumber, otp, purpose);
            return;
        }

        // Development / simulation mode
        latestDevOtps.put(cleanNumber, otp);

        log.info("==================================================");
        log.info("[SMS SERVICE - LOCAL/DEV SIMULATION MODE]");
        log.info("Mobile Number: {}", maskMobile(cleanNumber));
        log.info("Purpose: {}", purpose);
        log.info("OTP Code: {}", otp);
        log.info("Expiry: 5 minutes");
        log.info("Note: Set OTP_SMS_MODE=real and configure SMS provider credentials (Twilio or Fast2SMS) to enable live SMS delivery.");
        log.info("==================================================");
    }

    private void sendRealSms(String mobileNumber, String otp, String purpose) {
        String provider = smsProvider != null ? smsProvider.trim().toLowerCase() : "";

        if ("twilio".equals(provider)) {
            sendTwilioSms(mobileNumber, otp, purpose);
        } else if ("fast2sms".equals(provider)) {
            sendFast2Sms(mobileNumber, otp, purpose);
        } else {
            log.warn("Unrecognized or missing SMS provider [{}] in REAL mode.", smsProvider);
            throw new ApiException("Mobile OTP service is not configured. Please contact the administrator.", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private void sendTwilioSms(String mobileNumber, String otp, String purpose) {
        if (isBlank(twilioAccountSid) || isBlank(twilioAuthToken) || isBlank(twilioPhoneNumber)) {
            log.warn("Twilio SMS provider is selected in REAL mode (OTP_SMS_MODE=real), but TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, or TWILIO_PHONE_NUMBER is not configured.");
            throw new ApiException("Mobile OTP service is not configured. Please contact the administrator.", HttpStatus.SERVICE_UNAVAILABLE);
        }

        try {
            String url = "https://api.twilio.com/2010-04-01/Accounts/" + twilioAccountSid.trim() + "/Messages.json";
            String e164Number = "+91" + mobileNumber;
            String messageBody = "Your Hostel Management System verification OTP for " + purpose + " is " + otp + ". Valid for 5 minutes. Do not share this code.";

            String formPayload = "To=" + URLEncoder.encode(e164Number, StandardCharsets.UTF_8)
                    + "&From=" + URLEncoder.encode(twilioPhoneNumber.trim(), StandardCharsets.UTF_8)
                    + "&Body=" + URLEncoder.encode(messageBody, StandardCharsets.UTF_8);

            String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                    (twilioAccountSid.trim() + ":" + twilioAuthToken.trim()).getBytes(StandardCharsets.UTF_8)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", authHeader)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("SMS OTP successfully dispatched via Twilio to {}", maskMobile(mobileNumber));
            } else {
                log.error("Twilio SMS dispatch failed with HTTP status {}: {}", response.statusCode(), response.body());
                throw new ApiException("Failed to send mobile OTP via SMS provider. Please try again later.", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (ApiException ae) {
            throw ae;
        } catch (Exception e) {
            log.error("Exception occurred while sending SMS via Twilio: {}", e.getMessage());
            throw new ApiException("Failed to send mobile OTP. Please try again later.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void sendFast2Sms(String mobileNumber, String otp, String purpose) {
        if (isBlank(fast2smsApiKey)) {
            log.warn("Fast2SMS provider is selected in REAL mode (OTP_SMS_MODE=real), but FAST2SMS_API_KEY is not configured.");
            throw new ApiException("Mobile OTP service is not configured. Please contact the administrator.", HttpStatus.SERVICE_UNAVAILABLE);
        }

        try {
            String url = "https://www.fast2sms.com/dev/bulkV2";
            String jsonPayload = String.format(
                    "{\"route\":\"otp\",\"variables_values\":\"%s\",\"numbers\":\"%s\"}",
                    otp, mobileNumber
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("authorization", fast2smsApiKey.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("SMS OTP successfully dispatched via Fast2SMS to {}", maskMobile(mobileNumber));
            } else {
                log.error("Fast2SMS dispatch failed with HTTP status {}: {}", response.statusCode(), response.body());
                throw new ApiException("Failed to send mobile OTP via SMS provider. Please try again later.", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (ApiException ae) {
            throw ae;
        } catch (Exception e) {
            log.error("Exception occurred while sending SMS via Fast2SMS: {}", e.getMessage());
            throw new ApiException("Failed to send mobile OTP. Please try again later.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public String getLatestDevOtp(String mobileNumber) {
        if (mobileNumber == null) return null;
        String clean = normalizeMobileNumber(mobileNumber);
        return latestDevOtps.get(clean);
    }

    private String normalizeMobileNumber(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new ApiException("Please enter your registered mobile number", HttpStatus.BAD_REQUEST);
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() == 12 && digits.startsWith("91")) {
            digits = digits.substring(2);
        } else if (digits.length() == 11 && digits.startsWith("0")) {
            digits = digits.substring(1);
        }

        if (!INDIAN_MOBILE_PATTERN.matcher(digits).matches()) {
            throw new ApiException("Please enter a valid 10-digit Indian mobile number starting with 6-9.", HttpStatus.BAD_REQUEST);
        }
        return digits;
    }

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) return "****";
        return mobile.substring(0, 2) + "******" + mobile.substring(mobile.length() - 2);
    }
}
