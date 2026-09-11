import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ThemeProvider } from './context/ThemeContext';
import { ToastProvider } from './context/ToastContext';
import { ProtectedRoute } from './routes/ProtectedRoute';
import { ErrorBoundary } from './components/feedback/ErrorBoundary';

// Public Pages
import { Home } from './pages/Home';
import { Login } from './pages/Login';
import { Register } from './pages/Register';
import { Profile } from './pages/Profile';

// Admin Pages
import { AdminDashboard } from './pages/admin/AdminDashboard';
import { RoomManagement } from './pages/admin/RoomManagement';
import { BookingManagement } from './pages/admin/BookingManagement';
import { FeeManagement } from './pages/admin/FeeManagement';
import { UserManagement } from './pages/admin/UserManagement';
import { ComplaintManagement } from './pages/admin/ComplaintManagement';
import { LeaveManagement } from './pages/admin/LeaveManagement';
import { NoticeManagement } from './pages/admin/NoticeManagement';

// Student Pages
import { StudentDashboard } from './pages/student/StudentDashboard';
import { MyBookings } from './pages/student/MyBookings';
import { MyPayments } from './pages/student/MyPayments';
import { MyComplaints } from './pages/student/MyComplaints';
import { MyLeave } from './pages/student/MyLeave';
import { StudentNotices } from './pages/student/StudentNotices';

// Warden Pages
import { WardenDashboard } from './pages/warden/WardenDashboard';

export default function App() {
  return (
    <ErrorBoundary>
      <ThemeProvider>
        <AuthProvider>
          <ToastProvider>
            <BrowserRouter>
              <Routes>
                {/* Public Routes */}
                <Route path="/" element={<Home />} />
                <Route path="/login" element={<Login />} />
                <Route path="/register" element={<Register />} />

                {/* Admin Routes */}
                <Route element={<ProtectedRoute allowedRole="ADMIN" />}>
                  <Route path="/admin/dashboard" element={<AdminDashboard />} />
                  <Route path="/admin/rooms" element={<RoomManagement />} />
                  <Route path="/admin/bookings" element={<BookingManagement />} />
                  <Route path="/admin/fees" element={<FeeManagement />} />
                  <Route path="/admin/users" element={<UserManagement />} />
                  <Route path="/admin/complaints" element={<ComplaintManagement />} />
                  <Route path="/admin/leaves" element={<LeaveManagement />} />
                  <Route path="/admin/notices" element={<NoticeManagement />} />
                </Route>

                {/* Student Routes */}
                <Route element={<ProtectedRoute allowedRole="USER" />}>
                  <Route path="/student/dashboard" element={<StudentDashboard />} />
                  <Route path="/student/rooms" element={<MyBookings />} />
                  <Route path="/student/bookings" element={<MyBookings />} />
                  <Route path="/student/payments" element={<MyPayments />} />
                  <Route path="/student/complaints" element={<MyComplaints />} />
                  <Route path="/student/leaves" element={<MyLeave />} />
                  <Route path="/student/notices" element={<StudentNotices />} />
                </Route>

                {/* Warden Routes */}
                <Route element={<ProtectedRoute allowedRole="WARDEN" />}>
                  <Route path="/warden/dashboard" element={<WardenDashboard />} />
                  <Route path="/warden/students" element={<UserManagement isWarden />} />
                  <Route path="/warden/rooms" element={<RoomManagement isWarden />} />
                  <Route path="/warden/bookings" element={<BookingManagement isWarden />} />
                  <Route path="/warden/leaves" element={<LeaveManagement />} />
                  <Route path="/warden/complaints" element={<ComplaintManagement />} />
                  <Route path="/warden/notices" element={<NoticeManagement />} />
                </Route>

                {/* Shared Protected Route */}
                <Route element={<ProtectedRoute />}>
                  <Route path="/profile" element={<Profile />} />
                </Route>

                {/* Catch-all redirect */}
                <Route path="*" element={<Navigate to="/" replace />} />
              </Routes>
            </BrowserRouter>
          </ToastProvider>
        </AuthProvider>
      </ThemeProvider>
    </ErrorBoundary>
  );
}