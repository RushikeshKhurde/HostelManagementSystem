# OTP Configuration & Setup Guide

This document explains how to configure, run, and test the **Hostel Management System** with Email OTP delivery for both **Mandatory Student Registration Email Verification** and **Forgot Password / Password Reset**.

---

## 1. Features Overview

### A. Mandatory Student Registration Email Verification
Before a student account can be created in the system, email ownership must be verified:
1. **Send Verification OTP**: Student submits their email (`POST /api/auth/email-verification/send-otp`).
2. **OTP Dispatch**: A cryptographically secure 6-digit OTP is generated using `SecureRandom`, hashed via HMAC/SHA-256 with server pepper, and delivered to the recipient's inbox.
3. **Verify OTP**: Student submits the 6-digit code (`POST /api/auth/email-verification/verify-otp`).
4. **Registration Authorization Token**: Upon successful OTP verification, the backend issues a short-lived (10 min), single-use registration authorization token containing 256+ bits of entropy.
5. **Atomic Account Creation**: Student submits registration form with `registrationToken` (`POST /api/auth/register`). The backend validates and atomically consumes the authorization token in the database transaction, preventing token reuse or bypassing.

### B. Forgot Password / Password Reset
Self-service password recovery using Email OTP:
- `POST /api/auth/forgot-password/request`
- `POST /api/auth/forgot-password/resend`
- `POST /api/auth/forgot-password/verify-otp`
- `POST /api/auth/forgot-password/reset`

---

## 2. Delivery Modes

The application supports two delivery modes:
1. **Simulation Mode** (`OTP_EMAIL_MODE=simulation`): Default for local development and CI testing. OTPs are protected and handled in memory without requiring external SMTP credentials.
2. **Real SMTP Mode** (`OTP_EMAIL_MODE=real` or `OTP_EMAIL_MODE=smtp`): Sends real HTML/plain-text emails to recipient inboxes via Gmail SMTP or custom mail servers.

---

## 3. Real Gmail SMTP Configuration

### Step A: Generate a Google Gmail App Password
1. Log into your Google Account and visit **[Google Account Security](https://myaccount.google.com/security)**.
2. Ensure **2-Step Verification** is turned **ON**.
3. Under *2-Step Verification*, navigate to **App passwords** (or search "App passwords").
4. Create a new App Password (e.g. name it `HostelManagementSystem`).
5. Copy the 16-character generated code (e.g. `abcd efgh ijkl mnop`).

### Step B: Environment Variables in PowerShell
```powershell
$env:OTP_EMAIL_MODE="real"
$env:MAIL_HOST="smtp.gmail.com"
$env:MAIL_PORT="587"
$env:MAIL_USERNAME="your-email@gmail.com"
$env:MAIL_PASSWORD="your-16-char-app-password"
$env:MAIL_FROM="your-email@gmail.com"
```

---

## 4. API Endpoints Reference

### Student Registration Email Verification Endpoints

#### 1. Send Registration OTP
- **Method:** `POST`
- **URL:** `/api/auth/email-verification/send-otp`
- **Request Body:**
  ```json
  {
    "email": "student@example.com"
  }
  ```
- **Success Response (200 OK):**
  ```json
  {
    "message": "Verification code sent to your email address.",
    "email": "student@example.com",
    "destinationMasked": "s*****@example.com",
    "expiresInSeconds": 600,
    "resendCooldownSeconds": 60
  }
  ```

#### 2. Verify Registration OTP
- **Method:** `POST`
- **URL:** `/api/auth/email-verification/verify-otp`
- **Request Body:**
  ```json
  {
    "email": "student@example.com",
    "otp": "123456"
  }
  ```
- **Success Response (200 OK):**
  ```json
  {
    "message": "Email verified successfully.",
    "verified": true,
    "registrationToken": "550e8400-e29b-41d4-a716-446655440000-7c9e6679-7425-40de-944b-e07fc1f90ae7",
    "expiresInSeconds": 600
  }
  ```

#### 3. Complete Registration
- **Method:** `POST`
- **URL:** `/api/auth/register`
- **Request Body:**
  ```json
  {
    "fullName": "Rahul Sharma",
    "username": "rahul_sharma",
    "email": "student@example.com",
    "mobileNumber": "9876543210",
    "password": "SecurePassword@123",
    "confirmPassword": "SecurePassword@123",
    "gender": "Male",
    "dateOfBirth": "2002-05-15",
    "address": "Room 204, Block B",
    "registrationToken": "550e8400-e29b-41d4-a716-446655440000-7c9e6679-7425-40de-944b-e07fc1f90ae7"
  }
  ```
- **Success Response (200 OK):**
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "id": 5,
    "fullName": "Rahul Sharma",
    "username": "rahul_sharma",
    "email": "student@example.com",
    "mobileNumber": "9876543210",
    "role": "USER",
    "studentId": "STU-2026-00005",
    "emailVerified": true
  }
  ```

---

## 5. Security Controls & Rate Limits

| Parameter | Value | Behavior |
| :--- | :--- | :--- |
| **OTP Validity** | 10 minutes | OTP expires automatically after 10 minutes. |
| **Max Failed Attempts** | 5 attempts | Reaching 5 incorrect attempts invalidates OTP and locks for cooldown (429). |
| **Resend Cooldown** | 60 seconds | Enforces 60-second cooldown between consecutive requests. |
| **Email Rate Limit** | 5 per 15 min | Maximum 5 OTP requests per email in 15-minute sliding window. |
| **Token Entropy** | 256+ bits | Cryptographically secure UUID pair generated using `SecureRandom`. |
| **Token Lifetime** | 10 minutes | Registration authorization token expires after 10 minutes. |
| **Token Consumption** | Atomic Single-Use | Token is consumed atomically via conditional DB update; cannot be reused. |
| **Credential Safety** | Never logged | Plaintext OTPs, tokens, passwords, and SMTP secrets are never logged or stored. |

---

## 6. How to Run the Application

### Backend (Terminal 1)
```powershell
cd D:\STS\hostel-management-system\hostel-management-system
mvn spring-boot:run
```

### Frontend (Terminal 2)
```powershell
cd D:\STS\hostel-management-system\frontend
npm run dev
```

---

## 7. Automated Testing

Run the full automated test suite:
```powershell
cd D:\STS\hostel-management-system\hostel-management-system
mvn clean test
```
All 115 unit and integration tests execute with mocked mail sender and test H2 database in **100% BUILD SUCCESS**.
