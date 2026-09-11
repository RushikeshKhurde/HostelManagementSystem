import React, { useState } from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Navbar } from '../components/layout/Navbar';
import { Sidebar } from '../components/layout/Sidebar';
import { Footer } from '../components/layout/Footer';
import { LoadingSpinner } from '../components/feedback/LoadingSpinner';

export const ProtectedRoute = ({ allowedRole }) => {
  const { user, token, loading } = useAuth();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  if (loading) {
    return (
      <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <LoadingSpinner text="Authenticating session..." />
      </div>
    );
  }

  if (!token || !user) {
    return <Navigate to="/login" replace />;
  }

  const isRoleAllowed = () => {
    if (!allowedRole) return true;
    if (Array.isArray(allowedRole)) {
      return allowedRole.includes(user?.role);
    }
    return user?.role === allowedRole;
  };

  if (!isRoleAllowed()) {
    const fallback = user?.role === 'ADMIN' ? '/admin/dashboard' : user?.role === 'WARDEN' ? '/warden/dashboard' : '/student/dashboard';
    return <Navigate to={fallback} replace />;
  }

  return (
    <div className="app-layout">
      <Sidebar isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} />
      <div className="main-content">
        <Navbar onToggleSidebar={() => setSidebarOpen(!sidebarOpen)} />
        <main className="page-container">
          <Outlet />
        </main>
        <Footer />
      </div>
    </div>
  );
};