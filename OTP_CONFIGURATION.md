# OTP Configuration & Setup Guide

This document explains how to configure and run the **Hostel Management System** with Email OTP and Mobile SMS OTP delivery.

The application supports two delivery modes:
1. **Simulation Mode** (Default for development & automated tests — no external credentials required)
2. **Real Delivery Mode** (Sends real emails via SMTP and real SMS messages via Twilio or Fast2SMS)

---

## A. Development Simulation Mode

When running locally without external credentials, the system defaults to **Simulation Mode**.

- **Email**: OTPs are generated, hashed with BCrypt, and logged to the server console with full transaction details.
- **SMS**: OTPs are generated, hashed with BCrypt, and logged to the server console with masked mobile numbers.
- **Security**: In simulation mode, passwords and secrets are never logged.

### Default Environment Variables:
```properties
OTP_EMAIL_MODE=simulation
OTP_SMS_MODE=simulation
```

---

## B. Real Email SMTP Configuration

To enable live email delivery to students' inboxes, configure standard SMTP settings.

### Gmail SMTP Configuration
1. Enable 2-Step Verification on your Google Account.
2. Go to **Google Account Settings** -> **Security** -> **App passwords**.
3. Generate a new App Password (select "Mail" and "Windows Computer").
4. Copy the 16-character generated password.

### Required Environment Variables:
```bash
OTP_EMAIL_MODE=real
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-gmail-app-password
```

### Custom / College SMTP Configuration:
```bash
OTP_EMAIL_MODE=real
MAIL_HOST=smtp.yourcollege.edu
MAIL_PORT=587
MAIL_USERNAME=noreply@yourcollege.edu
MAIL_PASSWORD=your-smtp-password
```

> **Note**: If `OTP_EMAIL_MODE=real` is specified but `MAIL_USERNAME` or `MAIL_PASSWORD` is missing, the backend returns a safe HTTP 503 response:
> `"Email service is not configured. Please contact the administrator."`

---

## C. Real Mobile SMS Configuration

The system supports two enterprise SMS providers: **Twilio** and **Fast2SMS**.

### Option 1: Twilio SMS (International & India)
1. Sign up at [twilio.com](https://www.twilio.com) and access your Console.
2. Retrieve your **Account SID**, **Auth Token**, and active **Twilio Phone Number**.

#### Environment Variables:
```bash
OTP_SMS_MODE=real
SMS_PROVIDER=twilio
TWILIO_ACCOUNT_SID=ACXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
TWILIO_AUTH_TOKEN=your_twilio_auth_token_here
TWILIO_PHONE_NUMBER=+1234567890
```

### Option 2: Fast2SMS (India Bulk SMS / OTP)
1. Sign up at [fast2sms.com](https://www.fast2sms.com) and generate an API key.

#### Environment Variables:
```bash
OTP_SMS_MODE=real
SMS_PROVIDER=fast2sms
FAST2SMS_API_KEY=your_fast2sms_api_key_here
FAST2SMS_ROUTE=otp
```

> **Note**: If `OTP_SMS_MODE=real` is specified but the selected provider's credentials are not configured, the backend returns a safe HTTP 503 response:
> `"Mobile OTP service is not configured. Please contact the administrator."`

---

## D. Environment Variables Reference Table

| Variable | Required In | Default Value | Description |
|---|---|---|---|
| `OTP_EMAIL_MODE` | All | `simulation` | `simulation` or `real` |
| `OTP_SMS_MODE` | All | `simulation` | `simulation` or `real` |
| `MAIL_HOST` | Real Email | `smtp.gmail.com` | SMTP Server Host |
| `MAIL_PORT` | Real Email | `587` | SMTP Port (587 for TLS, 465 for SSL) |
| `MAIL_USERNAME` | Real Email | *(empty)* | Email Address / SMTP Username |
| `MAIL_PASSWORD` | Real Email | *(empty)* | App Password / SMTP Password |
| `SMS_PROVIDER` | Real SMS | `twilio` | `twilio` or `fast2sms` |
| `TWILIO_ACCOUNT_SID` | Real SMS (Twilio) | *(empty)* | Twilio Account SID |
| `TWILIO_AUTH_TOKEN` | Real SMS (Twilio) | *(empty)* | Twilio Authentication Token |
| `TWILIO_PHONE_NUMBER` | Real SMS (Twilio) | *(empty)* | Twilio Sender Phone Number (E.164) |
| `FAST2SMS_API_KEY` | Real SMS (Fast2SMS) | *(empty)* | Fast2SMS Authorization Key |
| `FAST2SMS_ROUTE` | Real SMS (Fast2SMS) | `otp` | Fast2SMS Route (`otp` or `q`) |

---

## E. How to Start the Application

### 1. Running in Default Simulation Mode (PowerShell)
```powershell
$env:JAVA_HOME = "C:\Users\HP\.jdks\ms-21.0.12.1"
$env:SPRING_PROFILES_ACTIVE = "local"
mvn spring-boot:run
```

### 2. Running with Real Email Delivery (PowerShell)
```powershell
$env:JAVA_HOME = "C:\Users\HP\.jdks\ms-21.0.12.1"
$env:SPRING_PROFILES_ACTIVE = "local"
$env:OTP_EMAIL_MODE = "real"
$env:MAIL_HOST = "smtp.gmail.com"
$env:MAIL_PORT = "587"
$env:MAIL_USERNAME = "your-email@gmail.com"
$env:MAIL_PASSWORD = "your-gmail-app-password"

mvn spring-boot:run
```

### 3. Running with Both Real Email and Real Twilio SMS (PowerShell)
```powershell
$env:JAVA_HOME = "C:\Users\HP\.jdks\ms-21.0.12.1"
$env:SPRING_PROFILES_ACTIVE = "local"

# Real Email
$env:OTP_EMAIL_MODE = "real"
$env:MAIL_HOST = "smtp.gmail.com"
$env:MAIL_PORT = "587"
$env:MAIL_USERNAME = "your-email@gmail.com"
$env:MAIL_PASSWORD = "your-gmail-app-password"

# Real SMS (Twilio)
$env:OTP_SMS_MODE = "real"
$env:SMS_PROVIDER = "twilio"
$env:TWILIO_ACCOUNT_SID = "ACXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
$env:TWILIO_AUTH_TOKEN = "your_twilio_auth_token_here"
$env:TWILIO_PHONE_NUMBER = "+1234567890"

mvn spring-boot:run
```

### 4. Running in IntelliJ IDEA
1. Open **Run/Debug Configurations** for `HostelManagementSystemApplication`.
2. Under **Environment variables**, paste your variables:
   ```
   SPRING_PROFILES_ACTIVE=local;OTP_EMAIL_MODE=real;MAIL_USERNAME=your-email@gmail.com;MAIL_PASSWORD=your-gmail-app-password
   ```
3. Click **Apply** and start the application.

---

## F. How to Test Email OTP

### Live Email Test Flow:
1. Open the browser to `http://localhost:8080/register`.
2. Enter your real email address (e.g. `yourname@gmail.com`).
3. Click **Send OTP**.
4. Check your email inbox for an email from **Hostel Management System** with subject `Hostel Management System - Student Registration Verification Code`.
5. Enter the 6-digit code shown in the styled card and click **Verify OTP**.
6. The green badge "Email verified successfully" will appear. Complete registration to get your sequential `STU-YYYY-XXXXX` student ID.

### Forgot Password via Email Test Flow:
1. Navigate to `http://localhost:8080/forgot-password`.
2. Select the **By Email** tab and enter your registered email.
3. Click **Send Verification OTP**.
4. Enter the 6-digit code received in your inbox.
5. Create a new strong password and confirm.
6. Login at `http://localhost:8080/login` with your new password.

---

## G. How to Test Mobile SMS OTP

### Live SMS Test Flow (Forgot Password):
1. Navigate to `http://localhost:8080/forgot-password`.
2. Select the **By Mobile** tab.
3. Enter your 10-digit Indian mobile number registered with your account (e.g. `9876543210`).
4. Click **Send Verification OTP**.
5. Check your phone SMS inbox for the message:
   `Your Hostel Management System verification OTP for Password Reset is 123456. Valid for 5 minutes. Do not share this code.`
6. Enter the 6-digit OTP in the web portal and set your new password.

---

## Security Notes
- **Never commit `.env` files** or real credentials to the Git repository.
- The `.gitignore` file is configured to ignore `.env`, `.env.*`, and `application-local.properties`.
- In real delivery mode, the backend **never logs OTP codes, passwords, or provider API keys** to disk or console.
