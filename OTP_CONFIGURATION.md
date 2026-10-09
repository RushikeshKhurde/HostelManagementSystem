# OTP Configuration & Setup Guide

This document explains how to configure and run the **Hostel Management System** with Email OTP delivery.

The application supports two delivery modes:
1. **Simulation Mode** (Default for development & automated tests — no external credentials required)
2. **Real Delivery Mode** (Sends real emails via SMTP / Gmail)

---

## A. Development Simulation Mode

When running locally without external credentials, the system defaults to **Simulation Mode**.

- **Email**: OTPs are generated, hashed with BCrypt, and logged to the server console with full transaction details.
- **Security**: In simulation mode, passwords and secrets are never logged.

### Default Environment Variables:
```properties
OTP_EMAIL_MODE=simulation
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

## C. Environment Variables Reference Table

| Variable | Required In | Default Value | Description |
|---|---|---|---|
| `OTP_EMAIL_MODE` | All | `simulation` | `simulation` or `real` |
| `MAIL_HOST` | Real Email | `smtp.gmail.com` | SMTP Server Host |
| `MAIL_PORT` | Real Email | `587` | SMTP Port (587 for TLS, 465 for SSL) |
| `MAIL_USERNAME` | Real Email | *(empty)* | Email Address / SMTP Username |
| `MAIL_PASSWORD` | Real Email | *(empty)* | App Password / SMTP Password |

---

## D. How to Start the Application

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

### 3. Running in IntelliJ IDEA
1. Open **Run/Debug Configurations** for `HostelManagementSystemApplication`.
2. Under **Environment variables**, paste your variables:
   ```
   SPRING_PROFILES_ACTIVE=local;OTP_EMAIL_MODE=real;MAIL_USERNAME=your-email@gmail.com;MAIL_PASSWORD=your-gmail-app-password
   ```
3. Click **Apply** and start the application.

---

## E. How to Test Email OTP

### Live Email Test Flow (Registration):
1. Open the browser to `http://localhost:8080/register`.
2. Enter your real email address (e.g. `yourname@gmail.com`).
3. Click **Send OTP**.
4. Check your email inbox for an email from **Hostel Management System** with subject `Hostel Management System - Student Registration Verification Code`.
5. Enter the 6-digit code shown in the styled card and click **Verify OTP**.
6. The green badge "Email verified successfully" will appear. Complete registration to get your sequential `STU-YYYY-XXXXX` student ID.

### Forgot Password via Email Test Flow:
1. Navigate to the login page and click **Forgot Password?**.
2. Enter your registered email address.
3. Click **Send OTP Code**.
4. Enter the 6-digit code received in your email inbox.
5. Create a new strong password and confirm.
6. Log in with your new password.

---

## Security Notes
- **Never commit `.env` files** or real credentials to the Git repository.
- The `.gitignore` file is configured to ignore `.env`, `.env.*`, and `application-local.properties`.
- In real delivery mode, the backend **never logs OTP codes, passwords, or provider API keys** to disk or console.
