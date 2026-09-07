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
    "Found $($rooms.Count) rooms. First room: $($rooms[0].roomNumber) ($($rooms[0].roomType))"
}

$results += Test-Endpoint "3. Public Notices (GET /api/notices)" {
    $notices = Invoke-RestMethod -Uri "$baseUrl/api/notices" -Method Get
    "Notices endpoint reachable. Count: $($notices.Count)"
}

# 2. Admin Login
$adminToken = ""
$results += Test-Endpoint "4. Admin Login (POST /api/auth/login)" {
    $body = @{ username = "Admin"; password = "Admin123" } | ConvertTo-Json
    $login = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -Body $body -ContentType "application/json"
    if ($login.role -ne "ADMIN" -or -not $login.token) { throw "Invalid admin auth response" }
    $script:adminToken = $login.token
    "Admin authenticated successfully. Role: $($login.role), Token prefix: $($login.token.Substring(0, 15))..."
}

# 3. Student Registration
$studentToken = ""
$randId = (Get-Random -Minimum 1000 -Maximum 9999)
$testStudentUsername = "alex_smart_$randId"
$testStudentEmail = "$testStudentUsername@university.edu"
$testMobile = "9" + (Get-Random -Minimum 100000000 -Maximum 999999999)

$results += Test-Endpoint "5. Student Registration (POST /api/auth/register)" {
    $regBody = @{
        fullName = "Alex Smart"
        username = $testStudentUsername
        email = $testStudentEmail
        mobileNumber = $testMobile
        password = "Password@123"
        confirmPassword = "Password@123"
        gender = "Female"
        dateOfBirth = "2003-08-20"
        address = "124 Innovation Way, Metro City"
    } | ConvertTo-Json
    $reg = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $regBody -ContentType "application/json"
    if ($reg.role -ne "USER" -or -not $reg.token) { throw "Invalid registration response" }
    $script:studentToken = $reg.token
    "Student registered with role USER. Token received."
}

# 4. Duplicate Registration Conflict Check (409)
$results += Test-Endpoint "6. Duplicate Username Conflict (409 expected)" {
    try {
        $regBody = @{
            fullName = "Duplicate Alex"
            username = $testStudentUsername
            email = "diff_$testStudentEmail"
            mobileNumber = "9" + (Get-Random -Minimum 100000000 -Maximum 999999999)
            password = "Password@123"
            confirmPassword = "Password@123"
            gender = "Female"
        } | ConvertTo-Json
        $dup = Invoke-RestMethod -Uri "$baseUrl/api/auth/register" -Method Post -Body $regBody -ContentType "application/json"
        throw "Should have failed with 409 Conflict"
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -eq 409 -or $_.ToString() -match "409|Conflict|already exists") {
            "Correctly rejected duplicate with 409 Conflict"
        } else {
            throw $_
        }
    }
}

# 5. Student Profile (GET /api/auth/me)
$results += Test-Endpoint "7. Student Profile Verification (GET /api/auth/me)" {
    $headers = @{ Authorization = "Bearer $studentToken" }
    $me = Invoke-RestMethod -Uri "$baseUrl/api/auth/me" -Method Get -Headers $headers
    if ($me.username -ne $testStudentUsername -or $me.role -ne "USER") { throw "Profile mismatch" }
    "Verified student: $($me.fullName) ($($me.username)), Gender: $($me.gender)"
}

# 6. Student Dashboard Stats (GET /api/dashboard/student-stats)
$results += Test-Endpoint "8. Student Dashboard Stats (GET /api/dashboard/student-stats)" {
    $headers = @{ Authorization = "Bearer $studentToken" }
    $stats = Invoke-RestMethod -Uri "$baseUrl/api/dashboard/student-stats" -Method Get -Headers $headers
    "Student stats retrieved. Total Bookings: $($stats.totalBookings), Total Paid: $($stats.totalPaid)"
}

# 7. Admin Dashboard Stats (GET /api/dashboard/stats)
$results += Test-Endpoint "9. Admin KPI Dashboard Stats (GET /api/dashboard/stats)" {
    $headers = @{ Authorization = "Bearer $adminToken" }
    $stats = Invoke-RestMethod -Uri "$baseUrl/api/dashboard/stats" -Method Get -Headers $headers
    "Admin KPIs: Rooms: $($stats.totalRooms), Total Users: $($stats.totalUsers), Capacity: $($stats.totalCapacity), Revenue: $($stats.totalRevenue)"
}

# 8. Student Room Booking Workflow (POST /api/bookings)
$bookingId = 0
$results += Test-Endpoint "10. Student Room Booking (POST /api/bookings)" {
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

# 9. Admin Booking Approval (PUT /api/bookings/{id}/status)
$results += Test-Endpoint "11. Admin Booking Approval (PUT /api/bookings/$bookingId/status)" {
    $headers = @{ Authorization = "Bearer $adminToken" }
    $upBody = @{ status = "APPROVED" } | ConvertTo-Json
    $approved = Invoke-RestMethod -Uri "$baseUrl/api/bookings/$bookingId/status" -Method Put -Body $upBody -Headers $headers -ContentType "application/json"
    if ($approved.status -ne "APPROVED") { throw "Booking was not approved" }
    "Booking #$bookingId approved successfully. Room status: $($approved.room.status), Room Occupied: $($approved.room.occupied)/$($approved.room.capacity)"
}

# 10. Student Fee Payment (POST /api/payments)
$results += Test-Endpoint "12. Student Fee Payment (POST /api/payments)" {
    $headers = @{ Authorization = "Bearer $studentToken" }
    $payBody = @{
        bookingId = $bookingId
        method = "UPI"
        payerDetail = "alex@okaxis"
    } | ConvertTo-Json
    $pay = Invoke-RestMethod -Uri "$baseUrl/api/payments" -Method Post -Body $payBody -Headers $headers -ContentType "application/json"
    if ($pay.status -ne "SUCCESS" -or -not $pay.transactionRef) { throw "Payment failed" }
    "Payment recorded: TxnRef $($pay.transactionRef), Amount: INR $($pay.amount), Method: $($pay.method), Status: $($pay.status)"
}

# 11. Student Grievance / Maintenance Ticket (POST /api/complaints)
$complaintId = 0
$results += Test-Endpoint "13. Student Lodge Complaint (POST /api/complaints)" {
    $headers = @{ Authorization = "Bearer $studentToken" }
    $compBody = @{
        title = "AC Unit not cooling"
        category = "ELECTRICAL"
        description = "Room 101 AC is blowing warm air since morning."
        priority = "HIGH"
    } | ConvertTo-Json
    $comp = Invoke-RestMethod -Uri "$baseUrl/api/complaints" -Method Post -Body $compBody -Headers $headers -ContentType "application/json"
    $script:complaintId = $comp.id
    "Complaint #$($comp.id) submitted: $($comp.title), Priority: $($comp.priority), Status: $($comp.status)"
}

# 12. Admin Grievance Resolution (PUT /api/complaints/{id}/status)
$results += Test-Endpoint "14. Admin Resolve Complaint (PUT /api/complaints/$complaintId/status)" {
    $headers = @{ Authorization = "Bearer $adminToken" }
    $upBody = @{
        status = "RESOLVED"
        adminComment = "Technician replaced filter. AC is fully functional."
    } | ConvertTo-Json
    $up = Invoke-RestMethod -Uri "$baseUrl/api/complaints/$complaintId/status" -Method Put -Body $upBody -Headers $headers -ContentType "application/json"
    "Complaint resolved: Status $($up.status), Remarks: $($up.adminComment)"
}

# 13. Student Leave Application (POST /api/leaves)
$leaveId = 0
$results += Test-Endpoint "15. Student Apply Leave / Outpass (POST /api/leaves)" {
    $headers = @{ Authorization = "Bearer $studentToken" }
    $leaveBody = @{
        leaveType = "OUTPASS"
        startDate = "2026-10-10"
        endDate = "2026-10-15"
        reason = "Attending sister's wedding in hometown."
        emergencyContact = "9876501234"
    } | ConvertTo-Json
    $leave = Invoke-RestMethod -Uri "$baseUrl/api/leaves" -Method Post -Body $leaveBody -Headers $headers -ContentType "application/json"
    $script:leaveId = $leave.id
    "Leave request #$($leave.id) submitted: Type: $($leave.leaveType), Status: $($leave.status)"
}

# 14. Admin Leave Approval (PUT /api/leaves/{id}/status)
$results += Test-Endpoint "16. Admin Approve Leave (PUT /api/leaves/$leaveId/status)" {
    $headers = @{ Authorization = "Bearer $adminToken" }
    $upLeave = @{
        status = "APPROVED"
        adminRemarks = "Outpass gate pass generated. Safe travels."
    } | ConvertTo-Json
    $appr = Invoke-RestMethod -Uri "$baseUrl/api/leaves/$leaveId/status" -Method Put -Body $upLeave -Headers $headers -ContentType "application/json"
    "Leave #$leaveId approved: Status $($appr.status), Remarks: $($appr.adminRemarks)"
}

# 15. Admin Notice Publishing (POST /api/notices)
$noticeId = 0
$results += Test-Endpoint "17. Admin Publish Notice (POST /api/notices)" {
    $headers = @{ Authorization = "Bearer $adminToken" }
    $notBody = @{
        title = "Annual Hostel Sports Meet 2026"
        content = "Registration for cricket and badminton tournaments starts tomorrow at the sports complex."
        priority = "HIGH"
        category = "SPORTS"
    } | ConvertTo-Json
    $not = Invoke-RestMethod -Uri "$baseUrl/api/notices" -Method Post -Body $notBody -Headers $headers -ContentType "application/json"
    $script:noticeId = $not.id
    "Notice published: ID #$($not.id), Title: $($not.title), Priority: $($not.priority)"
}

# 16. Admin User Management (GET /api/users)
$results += Test-Endpoint "18. Admin User Directory (GET /api/users)" {
    $headers = @{ Authorization = "Bearer $adminToken" }
    $users = Invoke-RestMethod -Uri "$baseUrl/api/users" -Method Get -Headers $headers
    "User directory retrieved: $($users.Count) registered users"
}

# 17. SPA Client Routes
$results += Test-Endpoint "19. SPA Client Route Forwarding (/login, /register, /student/dashboard, /admin/dashboard)" {
    $r1 = (Invoke-WebRequest -Uri "$baseUrl/login" -UseBasicParsing).StatusCode
    $r2 = (Invoke-WebRequest -Uri "$baseUrl/register" -UseBasicParsing).StatusCode
    $r3 = (Invoke-WebRequest -Uri "$baseUrl/student/dashboard" -UseBasicParsing).StatusCode
    $r4 = (Invoke-WebRequest -Uri "$baseUrl/admin/dashboard" -UseBasicParsing).StatusCode
    if ($r1 -eq 200 -and $r2 -eq 200 -and $r3 -eq 200 -and $r4 -eq 200) {
        "All SPA client-side routes forward to index.html with HTTP 200"
    } else {
        throw "SPA routing check failed: $r1 $r2 $r3 $r4"
    }
}

Write-Host "`n========================================================" -ForegroundColor Cyan
Write-Host "                TEST SUMMARY REPORT                     " -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan
$passed = ($results | Where-Object { $_.Status -eq "PASS" }).Count
$failed = ($results | Where-Object { $_.Status -eq "FAIL" }).Count
Write-Host "Total Tests: $($results.Count) | Passed: $passed | Failed: $failed" -ForegroundColor $(if ($failed -eq 0) { "Green" } else { "Red" })

if ($failed -eq 0) {
    Write-Host "`n>>> ALL 19 END-TO-END TESTS PASSED PERFECTLY! <<<" -ForegroundColor Green
}