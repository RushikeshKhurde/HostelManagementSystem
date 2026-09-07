import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import {
  LayoutDashboard,
  Building2,
  Bed,
  Users,
  CreditCard,
  MessageSquareWarning,
  CalendarDays,
  Bell,
  User,
  LogOut,
  X,
  Shield,
  BookmarkCheck,
} from 'lucide-react';

export const Sidebar = ({ isOpen, onClose }) => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  const adminNav = [
    { label: 'Overview', items: [
      { to: '/admin/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
    ]},
    { label: 'Management', items: [
      { to: '/admin/rooms', icon: Bed, label: 'Rooms & Hostels' },
      { to: '/admin/bookings', icon: BookmarkCheck, label: 'Booking Requests' },
      { to: '/admin/users', icon: Users, label: 'User Directory' },
    ]},
    { label: 'Finance', items: [
      { to: '/admin/fees', icon: CreditCard, label: 'Payments & Fees' },
    ]},
    { label: 'Student Services', items: [
      { to: '/admin/complaints', icon: MessageSquareWarning, label: 'Complaints' },
      { to: '/admin/leaves', icon: CalendarDays, label: 'Leave Requests' },
      { to: '/admin/notices', icon: Bell, label: 'Notice Board' },
    ]},
    { label: 'Account', items: [
      { to: '/profile', icon: User, label: 'My Profile' },
    ]},
  ];

  const studentNav = [
    { label: 'Overview', items: [
      { to: '/student/dashboard', icon: LayoutDashboard, label: 'My Dashboard' },
    ]},
    { label: 'Hostel Operations', items: [
      { to: '/student/rooms', icon: Bed, label: 'Explore Rooms' },
      { to: '/student/bookings', icon: BookmarkCheck, label: 'My Bookings' },
      { to: '/student/payments', icon: CreditCard, label: 'Fee Payments' },
    ]},
    { label: 'Services', items: [
      { to: '/student/complaints', icon: MessageSquareWarning, label: 'My Complaints' },
      { to: '/student/leaves', icon: CalendarDays, label: 'Leave Applications' },
      { to: '/student/notices', icon: Bell, label: 'Announcements' },
    ]},
    { label: 'Account', items: [
      { to: '/profile', icon: User, label: 'My Profile' },
    ]},
  ];

  const navSections = user?.role === 'ADMIN' ? adminNav : studentNav;

  return (
    <>
      {/* Mobile Backdrop */}
      {isOpen && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0,0,0,0.5)',
            zIndex: 99,
          }}
          onClick={onClose}
          className="sidebar-backdrop"
        />
      )}

      <aside
        style={{
          width: 'var(--sidebar-width)',
          backgroundColor: 'var(--bg-surface)',
          borderRight: '1px solid var(--border-color)',
          display: 'flex',
          flexDirection: 'column',
          height: '100vh',
          position: 'sticky',
          top: 0,
          zIndex: 100,
          transition: 'transform var(--transition-normal)',
        }}
        className={`sidebar ${isOpen ? 'sidebar-open' : ''}`}
      >
        {/* Brand Header */}
        <div
          style={{
            height: 'var(--navbar-height)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '0 1.25rem',
            borderBottom: '1px solid var(--border-color)',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
            <div
              style={{
                width: '34px',
                height: '34px',
                borderRadius: 'var(--radius-md)',
                background: 'linear-gradient(135deg, var(--primary) 0%, var(--accent) 100%)',
                color: '#ffffff',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontWeight: 800,
              }}
            >
              <Building2 size={20} />
            </div>
            <div>
              <div style={{ fontWeight: 800, fontSize: '1.0625rem', letterSpacing: '-0.02em', color: 'var(--text-primary)' }}>
                SmartHostel
              </div>
            </div>
          </div>
          <button
            onClick={onClose}
            className="btn btn-ghost btn-sm mobile-close-btn"
            style={{ padding: '4px' }}
            aria-label="Close menu"
          >
            <X size={18} />
          </button>
        </div>

        {/* Navigation Items */}
        <div style={{ flex: 1, overflowY: 'auto', padding: '1rem 0.75rem' }}>
          {navSections.map((section, idx) => (
            <div key={idx} style={{ marginBottom: '1.25rem' }}>
              <div
                style={{
                  fontSize: '0.6875rem',
                  fontWeight: 700,
                  textTransform: 'uppercase',
                  letterSpacing: '0.08em',
                  color: 'var(--text-muted)',
                  padding: '0.25rem 0.75rem 0.5rem',
                }}
              >
                {section.label}
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
                {section.items.map((item) => {
                  const Icon = item.icon;
                  return (
                    <NavLink
                      key={item.to}
                      to={item.to}
                      onClick={() => {
                        if (window.innerWidth < 1024) onClose();
                      }}
                      style={({ isActive }) => ({
                        display: 'flex',
                        alignItems: 'center',
                        gap: '0.75rem',
                        padding: '0.625rem 0.75rem',
                        borderRadius: 'var(--radius-md)',
                        fontSize: '0.875rem',
                        fontWeight: isActive ? 600 : 500,
                        color: isActive ? 'var(--primary)' : 'var(--text-secondary)',
                        backgroundColor: isActive ? 'var(--primary-light)' : 'transparent',
                        transition: 'all var(--transition-fast)',
                      })}
                      className="nav-link"
                    >
                      <Icon size={18} />
                      <span>{item.label}</span>
                    </NavLink>
                  );
                })}
              </div>
            </div>
          ))}
        </div>

        {/* Sidebar Footer */}
        <div style={{ padding: '0.75rem', borderTop: '1px solid var(--border-color)' }}>
          <button
            onClick={handleLogout}
            style={{
              width: '100%',
              display: 'flex',
              alignItems: 'center',
              gap: '0.75rem',
              padding: '0.625rem 0.75rem',
              borderRadius: 'var(--radius-md)',
              fontSize: '0.875rem',
              fontWeight: 500,
              color: 'var(--danger)',
              background: 'none',
              border: 'none',
              cursor: 'pointer',
              transition: 'background var(--transition-fast)',
            }}
            className="logout-link"
          >
            <LogOut size={18} />
            <span>Sign Out</span>
          </button>
        </div>
      </aside>
    </>
  );
};