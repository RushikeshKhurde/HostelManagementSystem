package com.hostel.management.service.delivery;

import com.hostel.management.model.User;
import com.hostel.management.model.VerificationChannel;

public interface OtpDeliveryService {

    VerificationChannel getSupportedChannel();

    void sendOtp(User user, String destination, String otp);
}
