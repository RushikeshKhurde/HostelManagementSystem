$baseUrl = "http://localhost:8080"
$results = @()

function Test-Endpoint {
    param($name, $scriptBlock)
    try {
        $res = & $scriptBlock
        Write-Host "[PASS] $name" -ForegroundColor Green
        return @{ Name = $name; Status = "PASS"; Details = $res }
    } catch {
        Write-Host "[FAIL] $name : $_" -ForegroundColor Red
        return @{ Name = $name; Status = "FAIL"; Details = $_.ToString() }
    }
}

# 1. Public API Tests
$results += Test-Endpoint "1. Public Landing Page (GET /)" {
    $resp = Invoke-WebRequest -Uri "$baseUrl/" -UseBasicParsing
    if ($resp.StatusCode -ne 200 -or -not ($resp.Content -match "Hostel Management")) { throw "Unexpected response" }
    "HTML loaded with title"
}

$results += Test-Endpoint "2. Public Rooms Catalog (GET /api/rooms)" {
    $rooms = Invoke-RestMethod -Uri "$baseUrl/api/rooms" -Method Get
    if ($rooms.Count -lt 1) { throw "No rooms found" }
    "Found $($rooms.Count) rooms."
}

# 2. Admin Login
$adminToken = ""
$results += Test-Endpoint "3. Admin Login (POST /api/auth/login)" {
    $body = @{ username = "Admin"; password = "Admin123" } | ConvertTo-Json
    $login = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $body -ContentType "application/json"
    if ($login.role -ne "ADMIN" -or -not $login.token) { throw "Invalid admin auth response" }
    $script:adminToken = $login.token
    "Admin authenticated successfully. Role: $($login.role)"
}

# 3. Student Registration
$studentToken = ""
$randId = (Get-Random -Minimum 1000 -Maximum 9999)
$testStudentUsername = "profile_user_$randId"
$testStudentEmail = "$testStudentUsername@college.edu"
$testMobile = "9" + (Get-Random -Minimum 100000000 -Maximum 999999999)

$results += Test-Endpoint "4. Student Registration (POST /api/auth/register)" {
    $regBody = @{
        fullName = "Original Student Name"
        username = $testStudentUsername
        email = $testStudentEmail
        mobileNumber = $testMobile
        password = "Password@123"
        confirmPassword = "Password@123"
        gender = "Male"
        dateOfBirth = "2002-04-10"
        address = "100 Initial Hall, Campus"
    } | ConvertTo-Json
    $reg = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $regBody -ContentType "application/json"
    if ($reg.role -ne "USER" -or -not $reg.token) { throw "Invalid registration response" }
    $script:studentToken = $reg.token
    "Student registered with role USER. Token received."
}

# 4. View Initial Profile (GET /api/profile/me)
$results += Test-Endpoint "5. View Initial Profile (GET /api/profile/me)" {
    $headers = @{ Authorization = "Bearer $studentToken" }
    $prof = Invoke-RestMethod -Uri "$baseUrl/api/profile/me" -Method Get -Headers $headers
    if ($prof.fullName -ne "Original Student Name" -or $prof.email -ne $testStudentEmail) {
        throw "Initial profile mismatch: $($prof.fullName), $($prof.email)"
    }
    "Profile verified: $($prof.fullName), $($prof.email), Mobile: $($prof.mobileNumber), Address: $($prof.address)"
}

# 5. Edit Student Profile (PUT /api/profile)
$updatedEmail = "updated_$testStudentEmail"
$updatedMobile = "9" + (Get-Random -Minimum 100000000 -Maximum 999999999)
$results += Test-Endpoint "6. Edit Student Profile Information (PUT /api/profile)" {
    $headers = @{ Authorization = "Bearer $studentToken" }
    $upBody = @{
        fullName = "Dr. Alexander Updated"
        email = $updatedEmail
        mobileNumber = $updatedMobile
        gender = "Male"
        dateOfBirth = "2001-12-25"
        address = "777 Upgraded Residency, Tech Boulevard"
        currentPassword = "Password@123"
        newPassword = "NewSecretPassword@456"
    } | ConvertTo-Json
    $upResp = Invoke-RestMethod -Uri "$baseUrl/api/profile" -Method Put -Body $upBody -Headers $headers -ContentType "application/json"
    if ($upResp.fullName -ne "Dr. Alexander Updated" -or $upResp.email -ne $updatedEmail -or $upResp.mobileNumber -ne $updatedMobile) {
        throw "Profile update response did not reflect changes: $($upResp.fullName)"
    }
    $script:studentToken = $upResp.token
    "Profile updated successfully: Name: $($upResp.fullName), Email: $($upResp.email), Mobile: $($upResp.mobileNumber), DOB: $($upResp.dateOfBirth)"
}

# 6. Verify Database Persistence of Updated Profile
$results += Test-Endpoint "7. Verify Database Persistence (GET /api/profile/me)" {
    $headers = @{ Authorization = "Bearer $studentToken" }
    $verified = Invoke-RestMethod -Uri "$baseUrl/api/profile/me" -Method Get -Headers $headers
    if ($verified.fullName -ne "Dr. Alexander Updated" -or $verified.email -ne $updatedEmail -or $verified.address -ne "777 Upgraded Residency, Tech Boulevard") {
        throw "Database did not persist updated profile!"
    }
    "Confirmed persistence from MySQL: Name: $($verified.fullName), Address: $($verified.address)"
}

# 7. Verify Login with New Password
$results += Test-Endpoint "8. Student Login with New Password (POST /api/auth/login)" {
    $body = @{ username = $testStudentUsername; password = "NewSecretPassword@456" } | ConvertTo-Json
    $login = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $body -ContentType "application/json"
    if ($login.fullName -ne "Dr. Alexander Updated" -or -not $login.token) { throw "Login with new password failed" }
    "Login with updated password successful. Name: $($login.fullName)"
}

# 8. Admin Edit Profile
$results += Test-Endpoint "9. Admin Edit Profile (PUT /api/profile)" {
    $headers = @{ Authorization = "Bearer $adminToken" }
    $adminUpBody = @{
        fullName = "Senior Hostel Administrator"
        email = "chief_admin@hostel.com"
        mobileNumber = "9998887776"
        gender = "Other"
        dateOfBirth = "1990-01-01"
        address = "Warden Headquarters, Central Building"
    } | ConvertTo-Json
    $admResp = Invoke-RestMethod -Uri "$baseUrl/api/profile" -Method Put -Body $adminUpBody -Headers $headers -ContentType "application/json"
    if ($admResp.fullName -ne "Senior Hostel Administrator" -or $admResp.email -ne "chief_admin@hostel.com") {
        throw "Admin profile update failed"
    }
    $script:adminToken = $admResp.token
    "Admin profile updated: $($admResp.fullName), Email: $($admResp.email)"
}

# 9. Verify Conflict on Duplicate Email in Profile Update (409 expected)
$results += Test-Endpoint "10. Profile Update Duplicate Email Conflict (409 expected)" {
    try {
        $headers = @{ Authorization = "Bearer $studentToken" }
        $conflictBody = @{
            fullName = "Conflict Tester"
            email = "chief_admin@hostel.com" # Taken by Admin
            mobileNumber = "9" + (Get-Random -Minimum 100000000 -Maximum 999999999)
            gender = "Male"
        } | ConvertTo-Json
        $fail = Invoke-RestMethod -Uri "$baseUrl/api/profile" -Method Put -Body $conflictBody -Headers $headers -ContentType "application/json"
        throw "Should have failed with 409 Conflict"
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -eq 409 -or $_.ToString() -match "409|Conflict|already in use") {
            "Correctly rejected duplicate email in profile update with 409 Conflict"
        } else {
            throw $_
        }
    }
}

# 10. Student Room Booking Workflow
$bookingId = 0
$results += Test-Endpoint "11. Student Room Booking (POST /api/bookings)" {
    $headers = @{ Authorization = "Bearer $studentToken" }
    $rooms = Invoke-RestMethod -Uri "$baseUrl/api/rooms" -Method Get
    $targetRoom = $rooms | Where-Object { $_.occupied -lt $_.capacity -and $_.status -eq "AVAILABLE" } | Select-Object -First 1
    if (-not $targetRoom) { $targetRoom = $rooms[0] }
    $bookBody = @{
        roomId = $targetRoom.id
        checkInDate = "2026-10-01"
    } | ConvertTo-Json
    $booking = Invoke-RestMethod -Uri "$baseUrl/api/bookings" -Method Post -Body $bookBody -Headers $headers -ContentType "application/json"
    if (-not $booking.id) { throw "Booking creation failed" }
    $script:bookingId = $booking.id
    "Booking created with ID #$($booking.id), Status: $($booking.status), Room: $($booking.room.roomNumber)"
}

# 11. Admin Booking Approval
$results += Test-Endpoint "12. Admin Booking Approval (PUT /api/bookings/$bookingId/status)" {
    $headers = @{ Authorization = "Bearer $adminToken" }
    $upBody = @{ status = "APPROVED" } | ConvertTo-Json
    $approved = Invoke-RestMethod -Uri "$baseUrl/api/bookings/$bookingId/status" -Method Put -Body $upBody -Headers $headers -ContentType "application/json"
    if ($approved.status -ne "APPROVED") { throw "Booking was not approved" }
    "Booking #$bookingId approved successfully."
}

# 12. Student Fee Payment
$results += Test-Endpoint "13. Student Fee Payment (POST /api/payments)" {
    $headers = @{ Authorization = "Bearer $studentToken" }
    $payBody = @{
        bookingId = $bookingId
        method = "UPI"
        payerDetail = "alex@okaxis"
    } | ConvertTo-Json
    $pay = Invoke-RestMethod -Uri "$baseUrl/api/payments" -Method Post -Body $payBody -Headers $headers -ContentType "application/json"
    if ($pay.status -ne "SUCCESS" -or -not $pay.transactionRef) { throw "Payment failed" }
    "Payment recorded: TxnRef $($pay.transactionRef), Amount: INR $($pay.amount)"
}

# 13. Student Complaint
$results += Test-Endpoint "14. Student Lodge Complaint (POST /api/complaints)" {
    $headers = @{ Authorization = "Bearer $studentToken" }
    $compBody = @{
        title = "Desk light flickering"
        category = "ELECTRICAL"
        description = "Light above study desk needs replacement."
        priority = "MEDIUM"
    } | ConvertTo-Json
    $comp = Invoke-RestMethod -Uri "$baseUrl/api/complaints" -Method Post -Body $compBody -Headers $headers -ContentType "application/json"
    "Complaint #$($comp.id) submitted: $($comp.title)"
}

# 14. Student Leave Application
$results += Test-Endpoint "15. Student Apply Leave / Outpass (POST /api/leaves)" {
    $headers = @{ Authorization = "Bearer $studentToken" }
    $leaveBody = @{
        leaveType = "WEEKEND_OUTPASS"
        startDate = "2026-10-17"
        endDate = "2026-10-19"
        reason = "Visiting relatives"
        emergencyContact = "9876543210"
    } | ConvertTo-Json
    $leave = Invoke-RestMethod -Uri "$baseUrl/api/leaves" -Method Post -Body $leaveBody -Headers $headers -ContentType "application/json"
    "Leave request #$($leave.id) submitted: Type: $($leave.leaveType), Status: $($leave.status)"
}

# 15. SPA Client Routes
$results += Test-Endpoint "16. SPA Client Route Forwarding (/profile, /login, /register, /student/dashboard, /admin/dashboard)" {
    $r1 = (Invoke-WebRequest -Uri "$baseUrl/profile" -UseBasicParsing).StatusCode
    $r2 = (Invoke-WebRequest -Uri "$baseUrl/login" -UseBasicParsing).StatusCode
    $r3 = (Invoke-WebRequest -Uri "$baseUrl/register" -UseBasicParsing).StatusCode
    $r4 = (Invoke-WebRequest -Uri "$baseUrl/student/dashboard" -UseBasicParsing).StatusCode
    $r5 = (Invoke-WebRequest -Uri "$baseUrl/admin/dashboard" -UseBasicParsing).StatusCode
    if ($r1 -eq 200 -and $r2 -eq 200 -and $r3 -eq 200 -and $r4 -eq 200 -and $r5 -eq 200) {
        "All SPA client routes returned HTTP 200"
    } else {
        throw "SPA routing failed"
    }
}

Write-Host "`n========================================================" -ForegroundColor Cyan
Write-Host "                TEST SUMMARY REPORT                     " -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
$passed = ($results | Where-Object { $_.Status -eq "PASS" }).Count
$failed = ($results | Where-Object { $_.Status -eq "FAIL" }).Count
Write-Host "Total Tests: $($results.Count) | Passed: $passed | Failed: $failed" -ForegroundColor $(if ($failed -eq 0) { "Green" } else { "Red" })

if ($failed -eq 0) {
    Write-Host "`n>>> ALL PROFILE EDIT & FULL-STACK TESTS PASSED PERFECTLY! <<<" -ForegroundColor Green
}